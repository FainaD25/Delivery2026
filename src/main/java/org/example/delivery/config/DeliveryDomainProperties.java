package org.example.delivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "delivery")
public class DeliveryDomainProperties {
    private Map<String, String> stations;
    private Map<String, BigDecimal> baseFees;

    public String stationFor(String city) {
        return stations.get(city);
    }

    public BigDecimal defaultBaseFee(String city, String vehicleType) {
        return baseFees.get(city + "|" + vehicleType);
    }
}
