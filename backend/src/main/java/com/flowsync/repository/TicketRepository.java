package com.flowsync.repository;
import com.flowsync.entity.Ticket;
import com.flowsync.enums.Priority;
import com.flowsync.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketKey(String key);
    List<Ticket> findByProject_Id(Long projectId);
    List<Ticket> findBySprint_Id(Long sprintId);
    List<Ticket> findByAssignee_Id(Long userId);
    List<Ticket> findByStatus(TicketStatus status);
    List<Ticket> findByProject_IdAndStatus(Long projectId, TicketStatus status);
    @Query("SELECT COALESCE(SUM(t.storyPoints),0) FROM Ticket t WHERE t.sprint.id = :sid AND t.status = 'CLOSED'")
    int sumCompletedPoints(@Param("sid") Long sprintId);
    @Query("SELECT t FROM Ticket t WHERE t.project.id = :pid AND (LOWER(t.title) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Ticket> searchInProject(@Param("pid") Long pid, @Param("q") String q);
}
