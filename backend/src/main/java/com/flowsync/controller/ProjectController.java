package com.flowsync.controller;
import com.flowsync.dto.request.CreateProjectRequest;
import com.flowsync.dto.response.*;
import com.flowsync.service.impl.ProjectServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/projects") @RequiredArgsConstructor
@Tag(name="Projects")
public class ProjectController {
    private final ProjectServiceImpl projectService;
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER','MANAGER')")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody CreateProjectRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.create(req)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(projectService.getAll()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.getById(id)));
    }
    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.addMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member added", null));
    }
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed", null));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.ok("Project deleted", null));
    }
}
