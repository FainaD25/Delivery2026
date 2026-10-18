package org.example.delivery.repository;

import org.example.delivery.entity.WeatherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface WeatherRepository extends JpaRepository<WeatherEntity, Long> {

    @Query("SELECT w FROM WeatherEntity w WHERE w.stationName = :stationName ORDER BY w.timestamp DESC LIMIT 1")
    WeatherEntity findLatestByStationName(String stationName);

    @Query("SELECT w FROM WeatherEntity w WHERE w.stationName = :stationName AND w.timestamp <= :timestamp ORDER BY w.timestamp DESC LIMIT 1")
    WeatherEntity findByStationNameAndTimestampBefore(String stationName, LocalDateTime timestamp);

    @Query("SELECT w FROM WeatherEntity w WHERE w.stationName = :stationName AND w.timestamp >= :timestamp ORDER BY w.timestamp ASC LIMIT 1")
    WeatherEntity findByStationNameAndTimestampAfter(String stationName, LocalDateTime timestamp);
}