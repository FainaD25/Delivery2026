package org.example.delivery.repository;

import org.example.delivery.entity.WeatherEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WeatherRepositoryTest {

    @Autowired
    private WeatherRepository weatherRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Test
    void testFindLatestByStationName_ReturnsMostRecent() {
        entityManager.createQuery("DELETE FROM WeatherEntity").executeUpdate();

        WeatherEntity oldest = new WeatherEntity();
        oldest.setStationName("Tallinn-Harku");
        oldest.setTimestamp(Instant.parse("2023-01-01T10:00:00Z"));
        oldest.setAirTemperature(5.0);
        oldest.setWindSpeed(5.0);
        oldest.setWeatherPhenomenon("Clear");

        WeatherEntity newest = new WeatherEntity();
        newest.setStationName("Tallinn-Harku");
        newest.setTimestamp(Instant.parse("2023-01-01T12:00:00Z"));
        newest.setAirTemperature(10.0);
        newest.setWindSpeed(10.0);
        newest.setWeatherPhenomenon("Rain");

        weatherRepository.save(oldest);
        weatherRepository.save(newest);

        WeatherEntity result = weatherRepository.findLatestByStationName("Tallinn-Harku");

        assertThat(result).isNotNull();
        assertThat(result.getTimestamp()).isEqualTo(Instant.parse("2023-01-01T12:00:00Z"));
        assertThat(result.getAirTemperature()).isEqualTo(10.0);
        assertThat(result.getWeatherPhenomenon()).isEqualTo("Rain");
    }

    @Transactional
    @Test
    void testFindByStationNameAndTimestampBefore_ReturnsCorrectRecord() {
        entityManager.createQuery("DELETE FROM WeatherEntity").executeUpdate();

        WeatherEntity record1 = new WeatherEntity();
        record1.setStationName("Tallinn-Harku");
        record1.setTimestamp(Instant.parse("2023-01-01T10:00:00Z"));
        record1.setAirTemperature(5.0);
        record1.setWindSpeed(5.0);
        record1.setWeatherPhenomenon("Clear");

        WeatherEntity record2 = new WeatherEntity();
        record2.setStationName("Tallinn-Harku");
        record2.setTimestamp(Instant.parse("2023-01-01T12:00:00Z"));
        record2.setAirTemperature(10.0);
        record2.setWindSpeed(10.0);
        record2.setWeatherPhenomenon("Rain");

        WeatherEntity record3 = new WeatherEntity();
        record3.setStationName("Tallinn-Harku");
        record3.setTimestamp(Instant.parse("2023-01-01T14:00:00Z"));
        record3.setAirTemperature(15.0);
        record3.setWindSpeed(15.0);
        record3.setWeatherPhenomenon("Snow");

        weatherRepository.save(record1);
        weatherRepository.save(record2);
        weatherRepository.save(record3);

        WeatherEntity result = weatherRepository.findByStationNameAndTimestampBefore(
                "Tallinn-Harku", Instant.parse("2023-01-01T11:30:00Z"));

        assertThat(result).isNotNull();
        assertThat(result.getTimestamp()).isEqualTo(Instant.parse("2023-01-01T10:00:00Z"));
        assertThat(result.getAirTemperature()).isEqualTo(5.0);
        assertThat(result.getWeatherPhenomenon()).isEqualTo("Clear");
    }

    @Transactional
    @Test
    void testFindByStationNameAndTimestampBefore_ReturnsNull_WhenNoEarlierData() {
        entityManager.createQuery("DELETE FROM WeatherEntity").executeUpdate();

        WeatherEntity record = new WeatherEntity();
        record.setStationName("Tallinn-Harku");
        record.setTimestamp(Instant.parse("2023-01-01T12:00:00Z"));
        record.setAirTemperature(10.0);
        record.setWindSpeed(10.0);
        record.setWeatherPhenomenon("Rain");

        weatherRepository.save(record);

        WeatherEntity result = weatherRepository.findByStationNameAndTimestampBefore(
                "Tallinn-Harku", Instant.parse("2023-01-01T10:00:00Z"));

        assertThat(result).isNull();
    }

    @Transactional
    @Test
    void testFindByStationNameAndTimestampBefore_ReturnsLatest_WhenTimestampIsAfterAllRecords() {
        entityManager.createQuery("DELETE FROM WeatherEntity").executeUpdate();

        WeatherEntity record1 = new WeatherEntity();
        record1.setStationName("Tallinn-Harku");
        record1.setTimestamp(Instant.parse("2023-01-01T10:00:00Z"));
        record1.setAirTemperature(5.0);
        record1.setWindSpeed(5.0);
        record1.setWeatherPhenomenon("Clear");

        WeatherEntity record2 = new WeatherEntity();
        record2.setStationName("Tallinn-Harku");
        record2.setTimestamp(Instant.parse("2023-01-01T12:00:00Z"));
        record2.setAirTemperature(10.0);
        record2.setWindSpeed(10.0);
        record2.setWeatherPhenomenon("Rain");

        weatherRepository.save(record1);
        weatherRepository.save(record2);

        WeatherEntity result = weatherRepository.findByStationNameAndTimestampBefore(
                "Tallinn-Harku", Instant.parse("2023-01-01T14:00:00Z"));

        assertThat(result).isNotNull();
        assertThat(result.getTimestamp()).isEqualTo(Instant.parse("2023-01-01T12:00:00Z"));
        assertThat(result.getAirTemperature()).isEqualTo(10.0);
        assertThat(result.getWeatherPhenomenon()).isEqualTo("Rain");
    }
}