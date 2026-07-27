package com.flowsync.dto.response;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
@Data @Builder
public class SprintResponse {
    private Long id;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer capacityPoints;
    private Integer completedPoints;
    private String status;
    private int progressPercent;
    private long totalTickets;
    private long closedTickets;
    private long inProgressTickets;
    private List<TicketResponse> tickets;
}
