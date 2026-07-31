package com.flowsync.repository;
import com.flowsync.entity.Project;
import com.flowsync.enums.ProjectStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface ProjectRepository extends MongoRepository<Project, Long> {
    Optional<Project> findByProjectKey(String key);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByOwner_Id(Long ownerId);
    
    default List<Project> findByMemberId(Long userId) {
        return findByMembers_Id(userId);
    }
    
    List<Project> findByMembers_Id(Long userId);
    boolean existsByProjectKey(String key);
}
