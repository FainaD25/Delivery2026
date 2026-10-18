package org.example.delivery.repository;

import org.example.delivery.entity.BaseFeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BaseFeeRepository extends JpaRepository<BaseFeeEntity, Long> {
    Optional<BaseFeeEntity> findByCityAndVehicleType(String city, String vehicleType);
}
