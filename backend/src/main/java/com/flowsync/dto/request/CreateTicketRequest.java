package com.flowsync.dto.request;
import com.flowsync.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
@Data
public class CreateTicketRequest {
    @NotBlank private String title;
    private String description;
    private Integer storyPoints;
    private Priority priority;
    private LocalDate dueDate;
    @NotNull private Long projectId;
    private Long sprintId;
    private Long assigneeId;
    private Long assignerId;
}
