package com.flowsync.entity;

import com.flowsync.enums.SprintStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "sprints")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Sprint extends BaseEntity {

    private String name;

    private String goal;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private Integer capacityPoints = 40;

    @Builder.Default
    private Integer completedPoints = 0;

    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNED;

    @DocumentReference
    private Project project;

    @DocumentReference(lazy = true)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    public int getProgressPercent() {
        if (capacityPoints == null || capacityPoints == 0) return 0;
        return (int) Math.round((completedPoints * 100.0) / capacityPoints);
    }
}
