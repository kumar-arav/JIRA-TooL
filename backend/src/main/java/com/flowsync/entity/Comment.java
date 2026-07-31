package com.flowsync.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment extends BaseEntity {
    private String content;

    @DocumentReference
    private Ticket ticket;

    @DocumentReference
    private User author;
}
