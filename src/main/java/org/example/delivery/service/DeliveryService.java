package org.example.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.delivery.entity.BaseFeeEntity;
import org.example.delivery.entity.WeatherEntity;
import org.example.delivery.repository.BaseFeeRepository;
import org.example.delivery.repository.WeatherRepository;
import org.example.delivery.util.VehicleType;
import org.example.delivery.util.WeatherPhenomenon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {
    private final WeatherRepository weatherRepository;
    private final BaseFeeRepository baseFeeRepository;

    private final Map<String, String> cityStationNames = Map.of(
            "Tallinn", "Tallinn-Harku",
            "Tartu", "Tartu-Tõravere",
            "Pärnu", "Pärnu"
    );

    @Value("${delivery.fee.cold-temperature-threshold:-10.0}")
    private double coldTemperatureThreshold;

    @Value("${delivery.fee.freezing-temperature-threshold:0.0}")
    private double freezingTemperatureThreshold;

    @Value("${delivery.fee.wind-speed-moderate-threshold:10.0}")
    private double windSpeedModerateThreshold;

    @Value("${delivery.fee.wind-speed-high-threshold:20.0}")
    private double windSpeedHighThreshold;

    @Value("${delivery.fee.cold-temperature-fee:0.5}")
    private double coldTemperatureFee;

    @Value("${delivery.fee.freezing-temperature-fee:1.0}")
    private double freezingTemperatureFee;

    @Value("${delivery.fee.wind-speed-fee:0.5}")
    private double windSpeedFee;

    @Value("${delivery.fee.precipitation-fee:0.5}")
    private double precipitationFee;

    @Value("${delivery.fee.severe-precipitation-fee:1.0}")
    private double severePrecipitationFee;

    /**
     * Calculates the air temperature based fee.
     *
     * @param entity the weather entity containing temperature data
     * @return the fee based on air temperature
     */
    private double calculateAirTemperatureFee(WeatherEntity entity) {
        double temperature = entity.getAirTemperature();

        if (temperature < coldTemperatureThreshold) {
            log.debug("Temperature {} is below threshold {}, applying freezing temperature fee: {}",
                    temperature, coldTemperatureThreshold, freezingTemperatureFee);
            return freezingTemperatureFee;
        } else if (temperature <= freezingTemperatureThreshold) {
            log.debug("Temperature {} is between {} and {}, applying cold temperature fee: {}",
                    temperature, coldTemperatureThreshold, freezingTemperatureThreshold, coldTemperatureFee);
            return coldTemperatureFee;
        }

        log.debug("Temperature {} is above freezing threshold {}, no temperature fee applied",
                temperature, freezingTemperatureThreshold);
        return 0.0;
    }

    /**
     * Calculates the wind speed based fee.
     *
     * @param entity the weather entity containing wind speed data
     * @return the fee based on wind speed
     * @throws IllegalArgumentException if wind speed is too high for certain vehicle types
     */
    private double calculateWindSpeedFee(WeatherEntity entity) {
        double windSpeed = entity.getWindSpeed();

        if (windSpeed > windSpeedHighThreshold) {
            log.debug("Wind speed {} exceeds high threshold {}, throwing forbidden exception",
                    windSpeed, windSpeedHighThreshold);
            throw new IllegalArgumentException("Usage of selected vehicle type is forbidden");
        } else if (windSpeed >= windSpeedModerateThreshold) {
            log.debug("Wind speed {} is at or above moderate threshold {}, applying wind speed fee: {}",
                    windSpeed, windSpeedModerateThreshold, windSpeedFee);
            return windSpeedFee;
        }

        log.debug("Wind speed {} is below moderate threshold {}, no wind speed fee applied",
                windSpeed, windSpeedModerateThreshold);
        return 0.0;
    }

    /**
     * Calculates the weather phenomenon based fee.
     *
     * @param entity the weather entity containing weather phenomenon data
     * @return the fee based on weather phenomenon
     * @throws IllegalArgumentException if weather phenomenon forbids certain vehicle types
     */
    private double calculateWeatherPhenomenonFee(WeatherEntity entity) {
        String phenomenon = entity.getWeatherPhenomenon();

        if (phenomenon == null || phenomenon.isEmpty()) {
            log.debug("Weather phenomenon is null or empty, no phenomenon fee applied");
            return 0.0;
        }

        String lowerCasePhenomenon = phenomenon.toLowerCase();

        if (WeatherPhenomenon.isSevereForbidden(lowerCasePhenomenon)) {
            log.debug("Weather phenomenon {} is severe forbidden, throwing forbidden exception",
                    phenomenon);
            throw new IllegalArgumentException("Usage of selected vehicle type is forbidden");
        }

        if (WeatherPhenomenon.isSevereFeeAll(lowerCasePhenomenon)) {
            log.debug("Weather phenomenon {} is severe for all vehicles, applying severe precipitation fee: {}",
                    phenomenon, severePrecipitationFee);
            return severePrecipitationFee;
        }

        if (WeatherPhenomenon.isModerateFee(lowerCasePhenomenon)) {
            log.debug("Weather phenomenon {} is moderate, applying precipitation fee: {}",
                    phenomenon, precipitationFee);
            return precipitationFee;
        }

        log.debug("Weather phenomenon {} does not match any fee criteria, no phenomenon fee applied",
                phenomenon);
        return 0.0;
    }

    /**
     * Calculates the extra fee based on weather conditions and vehicle type.
     * Cars are exempt from weather-based fees.
     *
     * @param entity the weather entity
     * @param vehicleTypeStr the vehicle type as string
     * @return the extra fee based on weather conditions
     */
    private double calculateExtraFee(WeatherEntity entity, String vehicleTypeStr) {
        if (vehicleTypeStr == null || vehicleTypeStr.isEmpty()) {
            throw new IllegalArgumentException("Vehicle type cannot be null or empty");
        }

        VehicleType vehicleType = VehicleType.fromString(vehicleTypeStr);
        double fee = 0.0;

        if (vehicleType != VehicleType.CAR) {
            log.debug("Calculating extra fees for vehicle type {} (not CAR)", vehicleType);

            double tempFee = calculateAirTemperatureFee(entity);
            double phenomenonFee = calculateWeatherPhenomenonFee(entity);

            fee += tempFee + phenomenonFee;
            log.debug("Temperature fee: {}, Phenomenon fee: {}, Subtotal: {}",
                    tempFee, phenomenonFee, fee);

            if (vehicleType == VehicleType.BIKE) {
                double windFee = calculateWindSpeedFee(entity);
                fee += windFee;
                log.debug("Wind speed fee: {}, Total fee: {}", windFee, fee);
            }
        } else {
            log.debug("Vehicle type is CAR, no weather-based fees applied");
        }

        return fee;
    }

    /**
     * Calculates the total delivery fee based on vehicle type, city, and current weather conditions.
     *
     * @param vehicleTypeStr the vehicle type as string
     * @param city the city for which to calculate the fee
     * @return the total delivery fee
     * @throws IllegalArgumentException if vehicle type or city is invalid
     * @throws IllegalStateException if weather data is not available
     */
    public double calculateTotalFee(String vehicleTypeStr, String city) {
        return calculateTotalFee(vehicleTypeStr, city, null);
    }

    /**
     * Calculates the total delivery fee based on vehicle type, city, and weather conditions at a specific time.
     *
     * @param vehicleTypeStr the vehicle type as string
     * @param city the city for which to calculate the fee
     * @param datetime the date and time for which to calculate the fee (optional, if null uses current time)
     * @return the total delivery fee
     * @throws IllegalArgumentException if vehicle type or city is invalid
     * @throws IllegalStateException if weather data is not available for the specified time
     */
    public double calculateTotalFee(String vehicleTypeStr, String city, String datetime) {
        if (vehicleTypeStr == null || vehicleTypeStr.isEmpty()) {
            throw new IllegalArgumentException("Vehicle type cannot be null or empty");
        }

        if (city == null || city.isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }

        log.info("Calculating delivery fee for vehicle type '{}' in city '{}'", vehicleTypeStr, city);

        String normalizedCity = null;
        for (String key : cityStationNames.keySet()) {
            if (key.equalsIgnoreCase(city)) {
                normalizedCity = key;
                break;
            }
        }

        if (normalizedCity == null) {
            log.warn("Invalid city provided: {}", city);
            throw new IllegalArgumentException("Invalid city provided.");
        }

        String stationName = cityStationNames.get(normalizedCity);

        WeatherEntity weatherToUse;
        if (datetime != null && !datetime.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                LocalDateTime queryTime = LocalDateTime.parse(datetime, formatter);

                weatherToUse = weatherRepository.findByStationNameAndTimestampBefore(stationName, queryTime);

                if (weatherToUse == null) {
                    weatherToUse = weatherRepository.findByStationNameAndTimestampAfter(stationName, queryTime);
                }
            } catch (Exception e) {
                log.warn("Failed to parse datetime '{}': {}, falling back to latest weather data", datetime, e.getMessage());
                weatherToUse = weatherRepository.findLatestByStationName(stationName);
            }
        } else {
            weatherToUse = weatherRepository.findLatestByStationName(stationName);
        }

        if (weatherToUse == null) {
            log.warn("No weather data available for station: {}", stationName);
            throw new IllegalStateException("No weather data available for " + city);
        }

        log.debug("Using weather data: {}", weatherToUse);

        VehicleType vehicleType = VehicleType.fromString(vehicleTypeStr);

        BaseFeeEntity baseFeeEntity = baseFeeRepository.findByCityAndVehicleType(city, vehicleType.getDisplayName())
                .orElseThrow(() -> {
                    log.warn("No base fee found for city: {} and vehicle type: {}", city, vehicleType.getDisplayName());
                    return new IllegalArgumentException("No base fee found for city and vehicle type.");
                });

        double baseFee = baseFeeEntity.getBaseFee();
        double extraFee = calculateExtraFee(weatherToUse, vehicleTypeStr);
        double totalFee = baseFee + extraFee;

        log.info("Calculated fee - Base: {}, Extra: {}, Total: {}", baseFee, extraFee, totalFee);

        return totalFee;
    }
}