package org.example.delivery.repository;

import org.example.delivery.entity.BaseFeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.Instant;

@Repository
public interface BaseFeeRepository extends JpaRepository<BaseFeeEntity, Long> {
    java.util.List<BaseFeeEntity> findAllByDeletedAtIsNull();
    java.util.List<BaseFeeEntity> findAllByCityAndVehicleTypeAndDeletedAtIsNull(String city, String vehicleType);

    Optional<BaseFeeEntity> findFirstByCityAndVehicleTypeAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(
            String city, String vehicleType);
    @Query("SELECT b FROM BaseFeeEntity b WHERE b.city = :city AND b.vehicleType = :vehicleType " +
            "AND (b.effectiveFrom IS NULL OR b.effectiveFrom <= :effectiveAt) " +
            "AND (b.deletedAt IS NULL OR b.deletedAt > :effectiveAt) " +
            "ORDER BY b.effectiveFrom DESC, b.id DESC")
    java.util.List<BaseFeeEntity> findByCityAndVehicleTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
            String city, String vehicleType, Instant effectiveAt);

    default Optional<BaseFeeEntity> findByCityAndVehicleType(String city, String vehicleType) {
        return findFirstByCityAndVehicleTypeAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(
                city, vehicleType);
    }
}
