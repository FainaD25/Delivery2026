package org.example.delivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "base_fees")
public class BaseFeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String vehicleType;

    @Column(nullable = false)
    private double baseFee;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
