package org.example.delivery.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Component
@ConfigurationProperties(prefix = "delivery.fee")
public class DeliveryFeeDefaults {
    private double coldTemperatureThreshold = -10.0;
    private double freezingTemperatureThreshold = 0.0;
    private double windSpeedModerateThreshold = 10.0;
    private double windSpeedHighThreshold = 20.0;
    private BigDecimal coldTemperatureFee = new BigDecimal("0.5");
    private BigDecimal freezingTemperatureFee = new BigDecimal("1.0");
    private BigDecimal windSpeedFee = new BigDecimal("0.5");
    private BigDecimal precipitationFee = new BigDecimal("0.5");
    private BigDecimal severePrecipitationFee = new BigDecimal("1.0");
}
