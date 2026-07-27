package com.flowsync.repository;
import com.flowsync.entity.Sprint;
import com.flowsync.enums.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProject_IdOrderByStartDateAsc(Long projectId);
    List<Sprint> findByStatus(SprintStatus status);
    Optional<Sprint> findByProject_IdAndStatus(Long projectId, SprintStatus status);
}
