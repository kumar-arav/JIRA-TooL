package com.flowsync.controller;
import com.flowsync.dto.request.*;
import com.flowsync.dto.response.*;
import com.flowsync.entity.User;
import com.flowsync.service.impl.TicketServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/tickets") @RequiredArgsConstructor
@Tag(name="Tickets")
public class TicketController {
    private final TicketServiceImpl ticketService;
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROJECT_OWNER','SCRUM_MASTER','MANAGER','DEVELOPER','TESTER','TRAINEE','CTO')")
    public ResponseEntity<ApiResponse<TicketResponse>> create(@Valid @RequestBody CreateTicketRequest req, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.createTicket(req, user != null ? user.getId() : null)));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROJECT_OWNER','SCRUM_MASTER','MANAGER','DEVELOPER','TESTER','TRAINEE','CTO')")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(@PathVariable Long id, @Valid @RequestBody CreateTicketRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.updateTicket(id, req)));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getById(id)));
    }
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getByProject(projectId)));
    }
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getBySprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getBySprint(sprintId)));
    }
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getMyTickets(user.getId())));
    }
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTicketStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.updateStatus(id, req)));
    }
    @PutMapping("/{id}/assignee")
    public ResponseEntity<ApiResponse<TicketResponse>> updateAssignee(@PathVariable Long id, @RequestBody java.util.Map<String, Long> req) {
        Long assigneeId = req.get("assigneeId");
        return ResponseEntity.ok(ApiResponse.ok(ticketService.updateAssignee(id, assigneeId)));
    }
    @PutMapping("/{id}/sprint")
    public ResponseEntity<ApiResponse<TicketResponse>> updateSprint(@PathVariable Long id, @RequestBody java.util.Map<String, Long> req) {
        Long sprintId = req.get("sprintId");
        return ResponseEntity.ok(ApiResponse.ok(ticketService.updateSprint(id, sprintId)));
    }
    @PutMapping("/{id}/approve/tester")
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    public ResponseEntity<ApiResponse<TicketResponse>> approveTester(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.approveTester(id)));
    }
    @PutMapping("/{id}/approve/manager")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<TicketResponse>> approveManager(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.approveManager(id)));
    }
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.addComment(id, req, user.getId())));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROJECT_OWNER','SCRUM_MASTER')")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket deleted successfully", null));
    }
}
