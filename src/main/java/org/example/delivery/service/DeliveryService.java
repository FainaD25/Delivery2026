package org.example.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.delivery.config.DeliveryDomainProperties;
import org.example.delivery.config.DeliveryFeeDefaults;
import org.example.delivery.entity.BaseFeeEntity;
import org.example.delivery.entity.WeatherEntity;
import org.example.delivery.repository.BaseFeeRepository;
import org.example.delivery.repository.WeatherRepository;
import org.example.delivery.util.VehicleType;
import org.example.delivery.util.WeatherPhenomenon;
import org.example.delivery.util.QueryTimeParser;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {
    private final WeatherRepository weatherRepository;
    private final BaseFeeRepository baseFeeRepository;
    private final FeeConfigurationService feeConfigurationService;
    private final DeliveryDomainProperties domainProperties;
    @Autowired
    private DeliveryFeeDefaults feeDefaults = new DeliveryFeeDefaults();

    private double getConfigurationValue(String key, double defaultValue, Instant effectiveAt) {
        if (effectiveAt == null) {
            return feeConfigurationService.getDoubleValue(key, defaultValue);
        }
        return feeConfigurationService.getDoubleValue(key, defaultValue, effectiveAt);
    }

    private double getColdTemperatureThreshold(Instant effectiveAt) {
        return getConfigurationValue(
                "delivery.fee.cold-temperature-threshold",
                feeDefaults.getColdTemperatureThreshold(), effectiveAt);
    }

    private double getFreezingTemperatureThreshold(Instant effectiveAt) {
        return getConfigurationValue(
                "delivery.fee.freezing-temperature-threshold",
                feeDefaults.getFreezingTemperatureThreshold(), effectiveAt);
    }

    private double getWindSpeedModerateThreshold(Instant effectiveAt) {
        return getConfigurationValue(
                "delivery.fee.wind-speed-moderate-threshold",
                feeDefaults.getWindSpeedModerateThreshold(), effectiveAt);
    }

    private double getWindSpeedHighThreshold(Instant effectiveAt) {
        return getConfigurationValue(
                "delivery.fee.wind-speed-high-threshold",
                feeDefaults.getWindSpeedHighThreshold(), effectiveAt);
    }

    private BigDecimal getColdTemperatureFee(Instant effectiveAt) {
        return BigDecimal.valueOf(getConfigurationValue(
                "delivery.fee.cold-temperature-fee",
                feeDefaults.getColdTemperatureFee().doubleValue(), effectiveAt));
    }

    private BigDecimal getFreezingTemperatureFee(Instant effectiveAt) {
        return BigDecimal.valueOf(getConfigurationValue(
                "delivery.fee.freezing-temperature-fee",
                feeDefaults.getFreezingTemperatureFee().doubleValue(), effectiveAt));
    }

    private BigDecimal getWindSpeedFee(Instant effectiveAt) {
        return BigDecimal.valueOf(getConfigurationValue(
                "delivery.fee.wind-speed-fee",
                feeDefaults.getWindSpeedFee().doubleValue(), effectiveAt));
    }

    private BigDecimal getPrecipitationFee(Instant effectiveAt) {
        return BigDecimal.valueOf(getConfigurationValue(
                "delivery.fee.precipitation-fee",
                feeDefaults.getPrecipitationFee().doubleValue(), effectiveAt));
    }

    private BigDecimal getSeverePrecipitationFee(Instant effectiveAt) {
        return BigDecimal.valueOf(getConfigurationValue(
                "delivery.fee.severe-precipitation-fee",
                feeDefaults.getSeverePrecipitationFee().doubleValue(), effectiveAt));
    }

    /**
     * Calculates the air temperature based fee.
     *
     * @param entity the weather entity containing temperature data
     * @return the fee based on air temperature
     */
    private double calculateAirTemperatureFee(WeatherEntity entity, Instant effectiveAt) {
        double temperature = entity.getAirTemperature();
        double coldThreshold = getColdTemperatureThreshold(effectiveAt);
        double freezingThreshold = getFreezingTemperatureThreshold(effectiveAt);
        BigDecimal coldFee = getColdTemperatureFee(effectiveAt);
        BigDecimal freezingFee = getFreezingTemperatureFee(effectiveAt);

        if (temperature < coldThreshold) {
            log.debug("Temperature {} is below threshold {}, applying freezing temperature fee: {}",
                    temperature, coldThreshold, freezingFee);
            return freezingFee.doubleValue();
        } else if (temperature <= freezingThreshold) {
            log.debug("Temperature {} is between {} and {}, applying cold temperature fee: {}",
                    temperature, coldThreshold, freezingThreshold, coldFee);
            return coldFee.doubleValue();
        }

        log.debug("Temperature {} is above freezing threshold {}, no temperature fee applied",
                temperature, freezingThreshold);
        return 0.0;
    }

    /**
     * Calculates the wind speed based fee.
     *
     * @param entity the weather entity containing wind speed data
     * @return the fee based on wind speed
     * @throws IllegalArgumentException if wind speed is too high for certain vehicle types
     */
    private double calculateWindSpeedFee(WeatherEntity entity, Instant effectiveAt) {
        double windSpeed = entity.getWindSpeed();
        double windSpeedHighThreshold = getWindSpeedHighThreshold(effectiveAt);
        double windSpeedModerateThreshold = getWindSpeedModerateThreshold(effectiveAt);
        BigDecimal windSpeedFee = getWindSpeedFee(effectiveAt);

        if (windSpeed > windSpeedHighThreshold) {
            log.debug("Wind speed {} exceeds high threshold {}, throwing forbidden exception",
                    windSpeed, windSpeedHighThreshold);
            throw new IllegalArgumentException("Usage of selected vehicle type is forbidden");
        } else if (windSpeed >= windSpeedModerateThreshold) {
            log.debug("Wind speed {} is at or above moderate threshold {}, applying wind speed fee: {}",
                    windSpeed, windSpeedModerateThreshold, windSpeedFee);
            return windSpeedFee.doubleValue();
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
    private double calculateWeatherPhenomenonFee(WeatherEntity entity, Instant effectiveAt) {
        String phenomenon = entity.getWeatherPhenomenon();

        if (phenomenon == null || phenomenon.isEmpty()) {
            log.debug("Weather phenomenon is null or empty, no phenomenon fee applied");
            return 0.0;
        }

        String lowerCasePhenomenon = phenomenon.toLowerCase();
        BigDecimal severePrecipitationFee = getSeverePrecipitationFee(effectiveAt);
        BigDecimal precipitationFee = getPrecipitationFee(effectiveAt);

        if (WeatherPhenomenon.isSevereForbidden(lowerCasePhenomenon)) {
            log.debug("Weather phenomenon {} is severe forbidden, throwing forbidden exception",
                    phenomenon);
            throw new IllegalArgumentException("Usage of selected vehicle type is forbidden");
        }

        if (WeatherPhenomenon.isSevereFeeAll(lowerCasePhenomenon)) {
            log.debug("Weather phenomenon {} is severe for all vehicles, applying severe precipitation fee: {}",
                    phenomenon, severePrecipitationFee);
            return severePrecipitationFee.doubleValue();
        }

        if (WeatherPhenomenon.isModerateFee(lowerCasePhenomenon)) {
            log.debug("Weather phenomenon {} is moderate, applying precipitation fee: {}",
                    phenomenon, precipitationFee);
            return precipitationFee.doubleValue();
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
    private double calculateExtraFee(WeatherEntity entity, String vehicleTypeStr, Instant effectiveAt) {
        if (vehicleTypeStr == null || vehicleTypeStr.isEmpty()) {
            throw new IllegalArgumentException("Vehicle type cannot be null or empty");
        }

        VehicleType vehicleType = VehicleType.fromString(vehicleTypeStr);
        double fee = 0.0;

        if (vehicleType != VehicleType.CAR) {
            log.debug("Calculating extra fees for vehicle type {} (not CAR)", vehicleType);

            double tempFee = calculateAirTemperatureFee(entity, effectiveAt);
            double phenomenonFee = calculateWeatherPhenomenonFee(entity, effectiveAt);

            fee += tempFee + phenomenonFee;
            log.debug("Temperature fee: {}, Phenomenon fee: {}, Subtotal: {}",
                    tempFee, phenomenonFee, fee);

            if (vehicleType == VehicleType.BIKE) {
                double windFee = calculateWindSpeedFee(entity, effectiveAt);
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
        for (String key : domainProperties.getStations().keySet()) {
            if (key.equalsIgnoreCase(city)) {
                normalizedCity = key;
                break;
            }
        }

        if (normalizedCity == null) {
            log.warn("Invalid city provided: {}", city);
            throw new IllegalArgumentException("Invalid city provided.");
        }

        String stationName = domainProperties.stationFor(normalizedCity);

        WeatherEntity weatherToUse;
        Instant effectiveAt = null;
        if (datetime != null && !datetime.isBlank()) {
            effectiveAt = QueryTimeParser.parse(datetime);
            weatherToUse = weatherRepository.findByStationNameAndTimestampBefore(stationName, effectiveAt);
            if (weatherToUse == null) {
                weatherToUse = weatherRepository.findByStationNameAndTimestampAfter(stationName, effectiveAt);
            }
        } else {
            weatherToUse = weatherRepository.findLatestByStationName(stationName);
        }

        if (weatherToUse == null) {
            log.warn("No weather data available for station: {}", stationName);
            throw new IllegalStateException("No weather data available for " + city);
        }
        if (weatherToUse.getAirTemperature() == null || weatherToUse.getWindSpeed() == null) {
            log.warn("Incomplete weather data available for station: {}", stationName);
            throw new IllegalStateException("Incomplete weather data available for " + city);
        }

        log.debug("Using weather data: {}", weatherToUse);

        VehicleType vehicleType = VehicleType.fromString(vehicleTypeStr);
        final String cityForLookup = normalizedCity;

        var baseFeeEntityResult = effectiveAt == null
                ? baseFeeRepository.findFirstByCityAndVehicleTypeAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(
                        cityForLookup, vehicleType.getDisplayName())
                : baseFeeRepository.findByCityAndVehicleTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        cityForLookup, vehicleType.getDisplayName(), effectiveAt)
                .stream()
                .findFirst();

        if (baseFeeEntityResult.isEmpty()) {
            var currentBaseFee = baseFeeRepository.findByCityAndVehicleType(
                    cityForLookup, vehicleType.getDisplayName());
            if (currentBaseFee.isPresent()
                    && (effectiveAt == null || currentBaseFee.get().getEffectiveFrom() == null)) {
                baseFeeEntityResult = currentBaseFee;
            }
        }

        BigDecimal baseFee = baseFeeEntityResult
                .map(BaseFeeEntity::getBaseFee)
                .map(BigDecimal::valueOf)
                .orElseGet(() -> {
                    var configuredDefault = domainProperties.defaultBaseFee(
                            cityForLookup, vehicleType.getDisplayName());
                    Double defaultFee = configuredDefault == null ? null : configuredDefault.doubleValue();
                    if (defaultFee == null) {
                        log.warn("No base fee or default found for city: {} and vehicle type: {}",
                                cityForLookup, vehicleType.getDisplayName());
                        throw new IllegalArgumentException("No base fee found for city and vehicle type.");
                    }
                    log.warn("No base fee found for city: {} and vehicle type: {}, using default: {}",
                            cityForLookup, vehicleType.getDisplayName(), defaultFee);
                    return BigDecimal.valueOf(defaultFee);
                });
        double extraFee = calculateExtraFee(weatherToUse, vehicleTypeStr, effectiveAt);
        double totalFee = baseFee.add(BigDecimal.valueOf(extraFee)).doubleValue();

        log.info("Calculated fee - Base: {}, Extra: {}, Total: {}", baseFee, extraFee, totalFee);

        return totalFee;
    }
}