package com.flowsync.dto.response;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Data @Builder
public class TicketResponse {
    private Long id;
    private String ticketKey;
    private String title;
    private String description;
    private Integer storyPoints;
    private String status;
    private String priority;
    private LocalDate dueDate;
    private UserResponse assignee;
    private UserResponse assigner;
    private UserResponse reporter;
    private String projectName;
    private String projectKey;
    private Long projectId;
    private String sprintName;
    private Long sprintId;
    private boolean testerApproved;
    private boolean managerApproved;
    private String closureNotes;
    private List<CommentResponse> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
