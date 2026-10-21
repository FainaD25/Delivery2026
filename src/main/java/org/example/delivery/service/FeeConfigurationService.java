package org.example.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.delivery.entity.FeeConfigurationEntity;
import org.example.delivery.config.DeliveryFeeDefaults;
import org.example.delivery.repository.FeeConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.util.Set;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeConfigurationService {
    private static final Set<String> NUMERIC_KEYS = Set.of(
            "delivery.fee.cold-temperature-threshold",
            "delivery.fee.freezing-temperature-threshold",
            "delivery.fee.wind-speed-moderate-threshold",
            "delivery.fee.wind-speed-high-threshold",
            "delivery.fee.cold-temperature-fee",
            "delivery.fee.freezing-temperature-fee",
            "delivery.fee.wind-speed-fee",
            "delivery.fee.precipitation-fee",
            "delivery.fee.severe-precipitation-fee"
    );
    private static final Set<String> FEE_KEYS = Set.of(
            "delivery.fee.cold-temperature-fee",
            "delivery.fee.freezing-temperature-fee",
            "delivery.fee.wind-speed-fee",
            "delivery.fee.precipitation-fee",
            "delivery.fee.severe-precipitation-fee"
    );

    private final FeeConfigurationRepository feeConfigurationRepository;
    @Autowired
    private DeliveryFeeDefaults feeDefaults = new DeliveryFeeDefaults();

    /**
     * Get a configuration value as a double.
     * @param key the configuration key
     * @param defaultValue the default value if key not found
     * @return the configuration value as double
     */
    public double getDoubleValue(String key, double defaultValue) {
        return getDoubleValue(key, defaultValue, null);
    }

    /**
     * Gets a configuration value valid at the supplied time.
     *
     * @param key the configuration key
     * @param defaultValue the default value if key not found
     * @param effectiveAt the calculation time, or null for the current value
     * @return the configuration value as double
     */
    public double getDoubleValue(String key, double defaultValue, Instant effectiveAt) {
        Optional<FeeConfigurationEntity> config = findConfiguration(key, effectiveAt);
        if (config.isPresent()) {
            try {
                BigDecimal value = new BigDecimal(config.get().getFeeValue());
                if (value.doubleValue() == Double.POSITIVE_INFINITY
                        || value.doubleValue() == Double.NEGATIVE_INFINITY) {
                    throw new NumberFormatException("Value is outside the supported range");
                }
                return value.doubleValue();
            } catch (NumberFormatException | ArithmeticException e) {
                log.warn("Invalid number format for key {}: {}, using default: {}",
                        key, config.get().getFeeValue(), defaultValue);
                return defaultValue;
            }
        }
        log.debug("Configuration key {} not found, using default: {}", key, defaultValue);
        return defaultValue;
    }

    /**
     * Get a configuration value as a String.
     * @param key the configuration key
     * @param defaultValue the default value if key not found
     * @return the configuration value as String
     */
    public String getStringValue(String key, String defaultValue) {
        Optional<FeeConfigurationEntity> config = findConfiguration(key, null);
        if (config.isPresent()) {
            return config.get().getFeeValue();
        }
        log.debug("Configuration key {} not found, using default: {}", key, defaultValue);
        return defaultValue;
    }

    /**
     * Get all configuration values as a map.
     * @return map of all configuration key-value pairs
     */
    public Map<String, String> getAllConfigurations() {
        Map<String, String> configMap = new HashMap<>();
        feeConfigurationRepository.findAll().stream()
                .filter(config -> config.getDeletedAt() == null)
                .sorted((left, right) -> {
                    Instant leftTime = left.getEffectiveFrom();
                    Instant rightTime = right.getEffectiveFrom();
                    if (leftTime == null && rightTime == null) {
                        return Long.compare(left.getId(), right.getId());
                    }
                    if (leftTime == null) {
                        return -1;
                    }
                    if (rightTime == null) {
                        return 1;
                    }
                    int timeComparison = leftTime.compareTo(rightTime);
                    return timeComparison != 0
                            ? timeComparison
                            : Long.compare(left.getId(), right.getId());
                })
                .forEach(config -> configMap.put(config.getFeeKey(), config.getFeeValue()));
        return configMap;
    }

    /**
     * Create or update a configuration value.
     * @param key the configuration key
     * @param value the configuration value
     * @param description optional description
     * @param updatedBy who made the update
     */
    public void setConfiguration(String key, String value,
                                                 String description, String updatedBy) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Configuration key is required");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Configuration value is required");
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            throw new IllegalArgumentException("Updated by is required");
        }
        if (NUMERIC_KEYS.contains(key)) {
            try {
                BigDecimal numericValue = new BigDecimal(value);
                if (FEE_KEYS.contains(key) && numericValue.signum() < 0) {
                    throw new IllegalArgumentException("Invalid numeric value for configuration: " + key);
                }
            } catch (NumberFormatException | ArithmeticException e) {
                throw new IllegalArgumentException("Invalid numeric value for configuration: " + key, e);
            }
            validateThresholds(key, new BigDecimal(value));
        }
        FeeConfigurationEntity config = new FeeConfigurationEntity();

        config.setFeeKey(key);
        config.setFeeValue(value);
        config.setDescription(description);
        config.setLastUpdated(Instant.now());
        config.setEffectiveFrom(config.getLastUpdated());
        config.setUpdatedBy(updatedBy);

        feeConfigurationRepository.save(config);
        log.info("Configuration updated: {} = {}", key, value);
    }

    /**
     * Get a configuration entity by key.
     * @param key the configuration key
     * @return the configuration entity, or null if not found
     */
    public FeeConfigurationEntity getConfiguration(String key) {
        return findConfiguration(key, null).orElse(null);
    }

    /**
     * Marks all active versions of a configuration key as deleted.
     *
     * @param key configuration key to delete
     */
    public void deleteConfiguration(String key) {
        Instant deletedAt = Instant.now();
        feeConfigurationRepository.findAllByFeeKeyAndDeletedAtIsNull(key).forEach(config -> {
            config.setDeletedAt(deletedAt);
            feeConfigurationRepository.save(config);
        });
    }

    private void validateThresholds(String key, BigDecimal value) {
        BigDecimal cold = valueForValidation(
                "delivery.fee.cold-temperature-threshold", key, value,
                BigDecimal.valueOf(feeDefaults.getColdTemperatureThreshold()));
        BigDecimal freezing = valueForValidation(
                "delivery.fee.freezing-temperature-threshold", key, value,
                BigDecimal.valueOf(feeDefaults.getFreezingTemperatureThreshold()));
        BigDecimal moderateWind = valueForValidation(
                "delivery.fee.wind-speed-moderate-threshold", key, value,
                BigDecimal.valueOf(feeDefaults.getWindSpeedModerateThreshold()));
        BigDecimal highWind = valueForValidation(
                "delivery.fee.wind-speed-high-threshold", key, value,
                BigDecimal.valueOf(feeDefaults.getWindSpeedHighThreshold()));

        if (cold.compareTo(freezing) >= 0) {
            throw new IllegalArgumentException("Cold temperature threshold must be lower than freezing threshold");
        }
        if (moderateWind.compareTo(highWind) > 0) {
            throw new IllegalArgumentException(
                    "Moderate wind threshold cannot exceed high wind threshold");
        }
    }

    private BigDecimal valueForValidation(String candidateKey, String changedKey,
                                          BigDecimal changedValue, BigDecimal defaultValue) {
        if (candidateKey.equals(changedKey)) {
            return changedValue;
        }
        return BigDecimal.valueOf(getDoubleValue(candidateKey, defaultValue.doubleValue()));
    }

    private Optional<FeeConfigurationEntity> findConfiguration(String key, Instant effectiveAt) {
        if (effectiveAt == null) {
            return feeConfigurationRepository
                    .findByFeeKeyAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(key)
                    .stream().findFirst();
        }
        return feeConfigurationRepository
                .findByFeeKeyAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(key, effectiveAt)
                .stream().findFirst();
    }
}