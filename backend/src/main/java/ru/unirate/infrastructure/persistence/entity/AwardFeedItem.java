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
@Table(name = "award_feed_items")
@Getter
@Setter
@NoArgsConstructor
public class AwardFeedItem {

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

    @Column(name = "activity_title", nullable = false)
    private String activityTitle;

    @Column(name = "teacher_name", nullable = false)
    private String teacherName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "award_id")
    private ActivityAward award;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
