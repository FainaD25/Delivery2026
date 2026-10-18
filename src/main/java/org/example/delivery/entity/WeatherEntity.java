package org.example.delivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private double airTemperature;
    @Column
    private double windSpeed;
    @Column
    private String weatherPhenomenon;
    @Column
    private LocalDateTime timestamp;
}

