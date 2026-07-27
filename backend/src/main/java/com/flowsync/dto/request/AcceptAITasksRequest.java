package com.flowsync.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AcceptAITasksRequest {
    @NotNull private Long sprintId;
    @NotNull private List<AITaskItem> tasks;

    @Data
    public static class AITaskItem {
        private String title;
        private String description;
        private Integer storyPoints;
        private String priority;       // CRITICAL|HIGH|MEDIUM|LOW
        private String suggestedRole;  // Developer / Tester — used for auto-assignment
    }
}