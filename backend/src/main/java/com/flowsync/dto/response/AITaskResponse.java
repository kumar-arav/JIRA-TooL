package com.flowsync.dto.response;
import lombok.*;
import java.util.List;
@Data @Builder
public class AITaskResponse {
    private List<AITask> tasks;
    private int totalPoints;
    private String generatedFor;
    @Data @Builder
    public static class AITask {
        private String title;
        private String description;
        private int storyPoints;
        private String priority;
        private String suggestedRole;
        private String type;
    }
}
