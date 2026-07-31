package com.flowsync.repository;
import com.flowsync.entity.Ticket;
import com.flowsync.enums.Priority;
import com.flowsync.enums.TicketStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface TicketRepository extends MongoRepository<Ticket, Long> {
    Optional<Ticket> findByTicketKey(String key);
    List<Ticket> findByProject_Id(Long projectId);
    List<Ticket> findBySprint_Id(Long sprintId);
    List<Ticket> findByAssignee_Id(Long userId);
    List<Ticket> findByStatus(TicketStatus status);
    List<Ticket> findByProject_IdAndStatus(Long projectId, TicketStatus status);
    
    default int sumCompletedPoints(Long sprintId) {
        return findBySprint_Id(sprintId).stream()
                .filter(t -> t.getStatus() == TicketStatus.CLOSED || t.getStatus().name().equals("CLOSED") || t.getStatus().name().equals("COMPLETED"))
                .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                .sum();
    }
    
    default List<Ticket> searchInProject(Long pid, String q) {
        return findByProject_Id(pid).stream()
                .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains(q.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
    }
}
