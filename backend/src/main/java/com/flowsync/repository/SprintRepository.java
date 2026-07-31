package com.flowsync.repository;
import com.flowsync.entity.Sprint;
import com.flowsync.enums.SprintStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface SprintRepository extends MongoRepository<Sprint, Long> {
    List<Sprint> findByProject_IdOrderByStartDateAsc(Long projectId);
    List<Sprint> findByStatus(SprintStatus status);
    Optional<Sprint> findByProject_IdAndStatus(Long projectId, SprintStatus status);
}
