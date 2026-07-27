package com.flowsync.entity;

import com.flowsync.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sprints")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Sprint extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(name = "capacity_points")
    @Builder.Default
    private Integer capacityPoints = 40;

    @Column(name = "completed_points")
    @Builder.Default
    private Integer completedPoints = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    public int getProgressPercent() {
        if (capacityPoints == null || capacityPoints == 0) return 0;
        return (int) Math.round((completedPoints * 100.0) / capacityPoints);
    }
}
