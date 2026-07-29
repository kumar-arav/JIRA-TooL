package com.flowsync.dto.response;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Data @Builder
public class ProjectResponse {
    private Long id;
    private String projectKey;
    private String name;
    private String description;
    private String emoji;
    private String status;
    private String priority;
    private LocalDate startDate;
    private LocalDate endDate;
    private String gitRepo;
    private String duration;
    private UserResponse owner;
    private List<UserResponse> members;
    private int totalTickets;
    private int openTickets;
    private int totalSprints;
    private int progressPercent;
    private LocalDateTime createdAt;
}
