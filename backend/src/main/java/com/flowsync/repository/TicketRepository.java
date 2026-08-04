package com.flowsync.repository;

import com.flowsync.entity.Ticket;
import com.flowsync.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketKey(String ticketKey);

    List<Ticket> findByProject_Id(Long projectId);

    List<Ticket> findBySprint_Id(Long sprintId);

    List<Ticket> findByAssignee_Id(Long userId);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByProject_IdAndStatus(Long projectId, TicketStatus status);

    default int sumCompletedPoints(Long sprintId) {
        return findBySprint_Id(sprintId).stream()
                .filter(t -> t.getStatus() == TicketStatus.CLOSED
                        || "COMPLETED".equals(t.getStatus().name()))
                .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                .sum();
    }

    default List<Ticket> searchInProject(Long projectId, String query) {
        return findByProject_Id(projectId).stream()
                .filter(t -> t.getTitle() != null &&
                        t.getTitle().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }
}