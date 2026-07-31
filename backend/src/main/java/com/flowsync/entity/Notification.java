package com.flowsync.entity;

import com.flowsync.enums.NotificationType;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseEntity {
    private NotificationType type;

    private String title;

    private String message;

    @Builder.Default
    private boolean read = false;

    @DocumentReference
    private User recipient;

    private Long relatedTicketId;
}
