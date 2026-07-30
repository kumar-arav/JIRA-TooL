package com.flowsync.controller;
import com.flowsync.dto.request.CreateSprintRequest;
import com.flowsync.dto.response.*;
import com.flowsync.service.impl.SprintServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/sprints") @RequiredArgsConstructor
@Tag(name="Sprints")
public class SprintController {
    private final SprintServiceImpl sprintService;
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<SprintResponse>> create(@Valid @RequestBody CreateSprintRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.create(req)));
    }
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.getByProject(projectId)));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SprintResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.getById(id)));
    }
    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<SprintResponse>> start(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.startSprint(id)));
    }
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<SprintResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.completeSprint(id)));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(@PathVariable Long id) {
        sprintService.deleteSprint(id);
        return ResponseEntity.ok(ApiResponse.ok("Sprint deleted", null));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(@PathVariable Long id, @Valid @RequestBody CreateSprintRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.updateSprint(id, req)));
    }
}
