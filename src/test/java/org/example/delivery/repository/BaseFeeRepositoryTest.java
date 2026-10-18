package org.example.delivery.repository;

import org.example.delivery.entity.BaseFeeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;

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
}