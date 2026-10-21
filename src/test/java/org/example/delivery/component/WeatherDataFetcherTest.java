package org.example.delivery.component;

import org.example.delivery.entity.WeatherEntity;
import org.example.delivery.exceptions.WeatherDataFetchException;
import org.example.delivery.repository.WeatherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class WeatherDataFetcherTest {
    private static final String WEATHER_URL =
            "https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php";

    @Test
    void fetchWeatherDataSavesOnlyTargetStationsAndConvertsTimestamp() {
        WeatherRepository repository = mock(WeatherRepository.class);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(WEATHER_URL))
                .andRespond(withStatus(OK).body("""
                        <observations timestamp="1700000000">
                          <station>
                            <name>Tallinn-Harku</name><wmocode>26038</wmocode>
                            <airtemperature>-2.5</airtemperature><windspeed>4.7</windspeed>
                            <phenomenon>Light snow shower</phenomenon>
                          </station>
                          <station>
                            <name>Other</name><wmocode>1</wmocode>
                            <airtemperature>1</airtemperature><windspeed>2</windspeed>
                            <phenomenon>Clear</phenomenon>
                          </station>
                        </observations>
                        """));

        new WeatherDataFetcher(repository, restTemplate).fetchWeatherData();

        ArgumentCaptor<WeatherEntity> captor = ArgumentCaptor.forClass(WeatherEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(WeatherEntity::getStationName)
                .containsExactly("Tallinn-Harku");
        assertThat(captor.getAllValues())
                .allMatch(entity -> entity.getTimestamp().toString().equals("2023-11-14T22:13:20Z"));
        server.verify();
    }

    @Test
    void fetchWeatherDataWrapsRemoteFailures() {
        WeatherRepository repository = mock(WeatherRepository.class);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(WEATHER_URL))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> new WeatherDataFetcher(repository, restTemplate).fetchWeatherData())
                .isInstanceOf(WeatherDataFetchException.class)
                .hasMessage("Weather data import failed")
                .hasCauseInstanceOf(Exception.class);
        verifyNoInteractions(repository);
        server.verify();
    }

    @Test
    void fetchWeatherDataRejectsExternalEntities() {
        WeatherRepository repository = mock(WeatherRepository.class);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(WEATHER_URL))
                .andRespond(withStatus(OK).body("""
                        <!DOCTYPE observations [
                          <!ENTITY external SYSTEM "file:///etc/passwd">
                        ]>
                        <observations timestamp="1700000000">
                          <station><name>&external;</name></station>
                        </observations>
                        """));

        assertThatThrownBy(() -> new WeatherDataFetcher(repository, restTemplate).fetchWeatherData())
                .isInstanceOf(WeatherDataFetchException.class);
        verifyNoInteractions(repository);
    }
}
