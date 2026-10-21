package org.example.delivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "fee_configuration")
public class FeeConfigurationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String feeKey;

    @Column(nullable = false)
    private String feeValue;

    @Column
    private String description;

    @Column(name = "last_updated", nullable = false)
    private java.time.Instant lastUpdated;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "effective_from")
    private java.time.Instant effectiveFrom;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;
}