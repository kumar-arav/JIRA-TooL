package com.flowsync.dto.request;
import lombok.Data;
@Data
public class AIGenerateRequest {
    private String projectDescription;
    private String projectType;
    private Long sprintId;
    private Integer taskCount;
}
