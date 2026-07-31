package com.flowsync.entity;

import com.flowsync.enums.Priority;
import com.flowsync.enums.TicketStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "tickets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ticket extends BaseEntity {

    private String ticketKey;   // e.g. "EHR-101"

    private String title;

    private String description;

    @Builder.Default
    private Integer storyPoints = 1;

    @Builder.Default
    private TicketStatus status = TicketStatus.TODO;

    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    private LocalDate dueDate;

    // Closure fields
    private String closureNotes;

    private String closureProofUrl;

    @Builder.Default
    private boolean testerApproved = false;

    @Builder.Default
    private boolean managerApproved = false;

    // Relations
    @DocumentReference
    private Project project;

    @DocumentReference
    private Sprint sprint;

    @DocumentReference
    private User assignee;

    @DocumentReference
    private User assigner;

    @DocumentReference
    private User reporter;

    @DocumentReference(lazy = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @DocumentReference(lazy = true)
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();
}
