package com.flowsync.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
@Data
public class CreateSprintRequest {
    @NotBlank private String name;
    private String goal;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private Integer capacityPoints;
    @NotNull private Long projectId;
}
