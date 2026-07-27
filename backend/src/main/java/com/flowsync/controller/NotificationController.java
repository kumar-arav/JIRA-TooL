package com.flowsync.controller;
import com.flowsync.dto.response.*;
import com.flowsync.entity.Notification;
import com.flowsync.entity.User;
import com.flowsync.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
@RestController @RequestMapping("/notifications") @RequiredArgsConstructor
@Tag(name="Notifications")
public class NotificationController {
    private final NotificationRepository notificationRepository;
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll(@AuthenticationPrincipal User user) {
        List<NotificationResponse> list = notificationRepository
            .findByRecipient_IdOrderByCreatedAtDesc(user.getId())
            .stream().map(this::map).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(notificationRepository.countByRecipient_IdAndReadFalse(user.getId())));
    }
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> { n.setRead(true); notificationRepository.save(n); });
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }
    private NotificationResponse map(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId()).type(n.getType().name()).title(n.getTitle())
            .message(n.getMessage()).read(n.isRead())
            .relatedTicketId(n.getRelatedTicketId()).createdAt(n.getCreatedAt()).build();
    }
}
