package com.flowsync.entity;

import com.flowsync.enums.Priority;
import com.flowsync.enums.ProjectStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "projects")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    private String projectKey;   // e.g. "EHR", "MOB"

    private String name;

    private String description;

    private String emoji;

    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    private LocalDate startDate;
    private LocalDate endDate;

    private String gitRepo;
    private String duration;

    @DocumentReference
    private User owner;

    @DocumentReference(lazy = true)
    @Builder.Default
    private List<User> members = new ArrayList<>();

    @DocumentReference(lazy = true)
    @Builder.Default
    private List<Sprint> sprints = new ArrayList<>();

    @DocumentReference(lazy = true)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();
}
