package org.example.delivery.repository;

import org.example.delivery.entity.FeeConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface FeeConfigurationRepository extends JpaRepository<FeeConfigurationEntity, Long> {
    @Query("SELECT f FROM FeeConfigurationEntity f WHERE f.feeKey = :feeKey AND f.deletedAt IS NULL " +
            "ORDER BY f.effectiveFrom DESC, f.id DESC")
    java.util.List<FeeConfigurationEntity> findByFeeKeyAndDeletedAtIsNullOrderByEffectiveFromDescIdDesc(String feeKey);
    @Query("SELECT f FROM FeeConfigurationEntity f WHERE f.feeKey = :feeKey " +
            "AND (f.effectiveFrom IS NULL OR f.effectiveFrom <= :effectiveAt) " +
            "AND (f.deletedAt IS NULL OR f.deletedAt > :effectiveAt) " +
            "ORDER BY f.effectiveFrom DESC, f.id DESC")
    java.util.List<FeeConfigurationEntity> findByFeeKeyAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
            String feeKey, Instant effectiveAt);
    java.util.List<FeeConfigurationEntity> findAllByFeeKeyAndDeletedAtIsNull(String feeKey);
}