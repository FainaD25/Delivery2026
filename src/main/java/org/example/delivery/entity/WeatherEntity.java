package org.example.delivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "weather_info")
public class WeatherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String stationName;
    @Column
    private String wmoCode;
    @Column
    private Double airTemperature;
    @Column
    private Double windSpeed;
    @Column
    private String weatherPhenomenon;
    @Column
    private Instant timestamp;
}
