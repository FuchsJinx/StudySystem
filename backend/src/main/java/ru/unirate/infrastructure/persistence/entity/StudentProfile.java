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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.unirate.domain.model.MembershipStatus;

import java.time.Instant;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false)
    private MembershipStatus membershipStatus = MembershipStatus.PENDING;

    @Column(name = "is_group_lead", nullable = false)
    private boolean groupLead;

    @Column(name = "academic_rating", nullable = false)
    private int academicRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_confirmed_by")
    private User membershipConfirmedBy;

    @Column(name = "membership_confirmed_at")
    private Instant membershipConfirmedAt;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt = Instant.now();

    public boolean isConfirmed() {
        return membershipStatus == MembershipStatus.CONFIRMED;
    }
}
