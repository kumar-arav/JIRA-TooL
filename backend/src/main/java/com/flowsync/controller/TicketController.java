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
    @PreAuthorize("hasAnyRole('ADMIN','SCRUM_MASTER','PROJECT_OWNER','MANAGER')")
    public ResponseEntity<ApiResponse<TicketResponse>> create(@Valid @RequestBody CreateTicketRequest req, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.createTicket(req, user != null ? user.getId() : null)));
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
}
