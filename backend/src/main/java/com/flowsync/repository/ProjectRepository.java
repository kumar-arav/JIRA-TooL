package com.flowsync.repository;

import com.flowsync.entity.Project;
import com.flowsync.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectKey(String projectKey);

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByOwner_Id(Long ownerId);

    List<Project> findByMembers_Id(Long userId);

    default List<Project> findByMemberId(Long userId) {
        return findByMembers_Id(userId);
    }

    boolean existsByProjectKey(String projectKey);
}