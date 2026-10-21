package org.example.delivery.repository;

import org.example.delivery.entity.BaseFeeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;

@DataJpaTest
class BaseFeeRepositoryTest {

    @Autowired
    private BaseFeeRepository baseFeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Test
    void testFindByCityAndVehicleType_ReturnsCorrectFee() {
        entityManager.createQuery("DELETE FROM BaseFeeEntity").executeUpdate();

        BaseFeeEntity tallinnCar = new BaseFeeEntity();
        tallinnCar.setCity("Tallinn");
        tallinnCar.setVehicleType("Car");
        tallinnCar.setBaseFee(4.0);

        BaseFeeEntity tartuBike = new BaseFeeEntity();
        tartuBike.setCity("Tartu");
        tartuBike.setVehicleType("Bike");
        tartuBike.setBaseFee(2.5);

        baseFeeRepository.save(tallinnCar);
        baseFeeRepository.save(tartuBike);

        BaseFeeEntity result1 = baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car").orElse(null);
        BaseFeeEntity result2 = baseFeeRepository.findByCityAndVehicleType("Tartu", "Bike").orElse(null);
        BaseFeeEntity result3 = baseFeeRepository.findByCityAndVehicleType("Pärnu", "Scooter").orElse(null); // Should not exist

        assertThat(result1).isNotNull();
        assertThat(result1.getBaseFee()).isEqualTo(4.0);

        assertThat(result2).isNotNull();
        assertThat(result2.getBaseFee()).isEqualTo(2.5);

        assertThat(result3).isNull();
    }

    @Test
    void testFindByCityAndVehicleType_ReturnsLatestActiveVersion() {
        BaseFeeEntity original = new BaseFeeEntity();
        original.setCity("Tallinn");
        original.setVehicleType("Car");
        original.setBaseFee(4.0);
        original.setEffectiveFrom(Instant.now().minusSeconds(60));

        BaseFeeEntity updated = new BaseFeeEntity();
        updated.setCity("Tallinn");
        updated.setVehicleType("Car");
        updated.setBaseFee(5.0);
        updated.setEffectiveFrom(Instant.now());

        baseFeeRepository.save(original);
        baseFeeRepository.save(updated);

        assertThat(baseFeeRepository.findByCityAndVehicleType("Tallinn", "Car"))
                .get()
                .extracting(BaseFeeEntity::getBaseFee)
                .isEqualTo(5.0);
    }

    @Test
    void testHistoricalLookupReturnsVersionValidAtRequestedTime() {
        Instant firstEffective = Instant.parse("2026-01-01T10:00:00Z");
        Instant secondEffective = Instant.parse("2026-02-01T10:00:00Z");

        BaseFeeEntity original = new BaseFeeEntity();
        original.setCity("Tallinn");
        original.setVehicleType("Car");
        original.setBaseFee(4.0);
        original.setEffectiveFrom(firstEffective);

        BaseFeeEntity updated = new BaseFeeEntity();
        updated.setCity("Tallinn");
        updated.setVehicleType("Car");
        updated.setBaseFee(5.0);
        updated.setEffectiveFrom(secondEffective);

        baseFeeRepository.save(original);
        baseFeeRepository.save(updated);

        var historicalVersions = baseFeeRepository
                .findByCityAndVehicleTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        "Tallinn", "Car", Instant.parse("2026-01-15T10:00:00Z"));
        assertThat(historicalVersions)
                .first()
                .extracting(BaseFeeEntity::getBaseFee)
                .isEqualTo(4.0);
    }

    @Test
    void testHistoricalLookupExcludesVersionAfterDeletion() {
        entityManager.createQuery("DELETE FROM BaseFeeEntity").executeUpdate();

        BaseFeeEntity deleted = new BaseFeeEntity();
        deleted.setCity("Tallinn");
        deleted.setVehicleType("Car");
        deleted.setBaseFee(5.0);
        deleted.setEffectiveFrom(Instant.parse("2026-01-01T10:00:00Z"));
        deleted.setDeletedAt(Instant.parse("2026-02-01T10:00:00Z"));
        baseFeeRepository.save(deleted);

        assertThat(baseFeeRepository
                .findByCityAndVehicleTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        "Tallinn", "Car", Instant.parse("2026-02-02T10:00:00Z")))
                .isEmpty();
    }
}