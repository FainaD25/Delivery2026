package org.example.delivery.component;

import org.example.delivery.entity.WeatherEntity;
import org.example.delivery.exceptions.WeatherDataFetchException;
import org.example.delivery.repository.WeatherRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import lombok.extern.slf4j.Slf4j;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class WeatherDataFetcher {
    private static final String WEATHER_API_URL = "https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php";
    private static final List<String> TARGET_STATIONS = List.of("Tallinn-Harku", "Tartu-Tõravere", "Pärnu");

    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public WeatherDataFetcher(WeatherRepository weatherDataRepository) {
        this(weatherDataRepository, new RestTemplate());
    }

    WeatherDataFetcher(WeatherRepository weatherDataRepository, RestTemplate restTemplate) {
        this.weatherRepository = weatherDataRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Imports the latest observations for the configured target stations.
     *
     * @throws WeatherDataFetchException when the remote response cannot be fetched or parsed
     */
    @Scheduled(cron = "${weather.fetch.cron:0 15 * * * *}")
    public void fetchWeatherData() {
        if (log.isInfoEnabled()) {
            log.info("Fetching weather data at {}", System.currentTimeMillis());
        }
        try {
            String xmlResponse = restTemplate.getForObject(WEATHER_API_URL, String.class);
            parseAndSaveWeatherData(xmlResponse);
        } catch (RestClientException | ParserConfigurationException | SAXException
                 | IOException | NumberFormatException e) {
            throw new WeatherDataFetchException("Weather data import failed", e);
        }
    }

    private void parseAndSaveWeatherData(String xmlResponse)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlResponse)));

        Element observationsElement = document.getDocumentElement();
        String obsTimestampStr = observationsElement.getAttribute("timestamp");
        long obsTimestampLong = Long.parseLong(obsTimestampStr);

        Instant obsInstant = Instant.ofEpochSecond(obsTimestampLong);
        java.time.Instant observationTime = obsInstant;

        NodeList stationNodes = document.getElementsByTagName("station");

        for (int i = 0; i < stationNodes.getLength(); i++) {
            Element stationElement = (Element) stationNodes.item(i);
            String stationName = getTextValue(stationElement, "name");

            if (TARGET_STATIONS.contains(stationName)) {
                WeatherEntity weatherEntity = new WeatherEntity();
                weatherEntity.setStationName(stationName);
                weatherEntity.setWmoCode(getTextValue(stationElement, "wmocode"));
                String tempStr = getTextValue(stationElement, "airtemperature");
                String windStr = getTextValue(stationElement, "windspeed");
                String phenomenonStr = getTextValue(stationElement, "phenomenon");

                weatherEntity.setAirTemperature(tempStr.isEmpty() ? null : Double.parseDouble(tempStr));
                weatherEntity.setWindSpeed(windStr.isEmpty() ? null : Double.parseDouble(windStr));
                weatherEntity.setWeatherPhenomenon(phenomenonStr.isEmpty() ? null : phenomenonStr);
                weatherEntity.setTimestamp(observationTime);

                weatherRepository.save(weatherEntity);
                if (log.isInfoEnabled()) {
                    log.info("Saved weather data for {} with observation time {}",
                            stationName, observationTime);
                }
            }
        }
    }

    private String getTextValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent() : "";
    }
}