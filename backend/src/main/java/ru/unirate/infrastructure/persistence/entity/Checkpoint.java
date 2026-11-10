package ru.unirate.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "checkpoints")
@Getter
@Setter
@NoArgsConstructor
public class Checkpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    @Column(nullable = false)
    private Instant deadline;

    @Column(nullable = false)
    private int cost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private TeacherProfile createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "checkpoint_groups",
            joinColumns = @JoinColumn(name = "checkpoint_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<StudentGroup> groups = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isDeadlinePassed(Instant now) {
        return now.isAfter(deadline);
    }
}
