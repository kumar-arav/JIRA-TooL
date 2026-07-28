package com.flowsync.dto.request;
import com.flowsync.enums.Priority;
import com.flowsync.enums.ProjectStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
@Data
public class CreateProjectRequest {
    @NotBlank @Size(max=10) private String projectKey;
    @NotBlank private String name;
    private String description;
    private String emoji;
    private Priority priority;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long ownerId;
    private Long scrumMasterId;
}
