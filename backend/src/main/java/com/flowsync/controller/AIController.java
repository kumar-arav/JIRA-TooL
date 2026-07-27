package com.flowsync.controller;
import com.flowsync.dto.request.AIGenerateRequest;
import com.flowsync.dto.request.AcceptAITasksRequest;
import com.flowsync.dto.response.*;
import com.flowsync.service.impl.AIServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/ai") @RequiredArgsConstructor
@Tag(name="AI Planner")
public class AIController {
    private final AIServiceImpl aiService;
    @PostMapping("/generate-tasks")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<AITaskResponse>> generateTasks(@RequestBody AIGenerateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.generateTasks(req)));
    }
    @PostMapping("/accept-tasks")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<String>> acceptTasks(@RequestBody AcceptAITasksRequest req) {
        aiService.acceptTasks(req);
        return ResponseEntity.ok(ApiResponse.ok("Tasks successfully imported into sprint backlog!"));
    }
}
