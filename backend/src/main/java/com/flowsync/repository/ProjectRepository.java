package com.flowsync.repository;
import com.flowsync.entity.Project;
import com.flowsync.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByProjectKey(String key);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByOwner_Id(Long ownerId);
    @Query("SELECT p FROM Project p JOIN p.members m WHERE m.id = :userId")
    List<Project> findByMemberId(@Param("userId") Long userId);
    boolean existsByProjectKey(String key);
}
