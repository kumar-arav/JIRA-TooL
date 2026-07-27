package com.flowsync.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private Long relatedTicketId;
    private LocalDateTime createdAt;
}
