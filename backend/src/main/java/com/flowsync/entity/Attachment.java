package com.flowsync.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attachment extends BaseEntity {
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;

    @DocumentReference
    private Ticket ticket;

    @DocumentReference
    private User uploadedBy;
}
