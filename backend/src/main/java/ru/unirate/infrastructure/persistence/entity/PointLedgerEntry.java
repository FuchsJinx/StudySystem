package ru.unirate.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.unirate.domain.model.PointCurrency;

import java.time.Instant;

@Entity
@Table(name = "point_ledger")
@Getter
@Setter
@NoArgsConstructor
public class PointLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PointCurrency currency;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(name = "ref_type", length = 64)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
