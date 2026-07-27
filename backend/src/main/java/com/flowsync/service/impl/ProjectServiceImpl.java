package com.flowsync.service.impl;

import com.flowsync.dto.request.CreateProjectRequest;
import com.flowsync.dto.response.ProjectResponse;
import com.flowsync.dto.response.UserResponse;
import com.flowsync.entity.Project;
import com.flowsync.entity.User;
import com.flowsync.enums.ProjectStatus;
import com.flowsync.exception.ResourceNotFoundException;
import com.flowsync.repository.ProjectRepository;
import com.flowsync.repository.TicketRepository;
import com.flowsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketServiceImpl ticketService;

    public ProjectResponse create(CreateProjectRequest req) {
        if (projectRepository.existsByProjectKey(req.getProjectKey().toUpperCase())) {
            throw new IllegalArgumentException("Project key already exists: " + req.getProjectKey());
        }
        Project project = Project.builder()
                .projectKey(req.getProjectKey().toUpperCase())
                .name(req.getName())
                .description(req.getDescription())
                .emoji(req.getEmoji() != null ? req.getEmoji() : "📋")
                .status(req.getStatus() != null ? req.getStatus() : ProjectStatus.PLANNING)
                .priority(req.getPriority())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();

        if (req.getOwnerId() != null) {
            project.setOwner(userRepository.findById(req.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.getOwnerId())));
        }
        ProjectResponse resp = mapToResponse(projectRepository.save(project));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");
        return resp;
    }

    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ProjectResponse getById(Long id) {
        return mapToResponse(projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id)));
    }

    public void addMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!project.getMembers().contains(user)) {
            project.getMembers().add(user);
            projectRepository.save(project);
            com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");
        }
    }

    private ProjectResponse mapToResponse(Project p) {
        List<UserResponse> members = p.getMembers().stream()
                .map(ticketService::mapUser).collect(Collectors.toList());

        int totalTickets = p.getTickets() != null ? p.getTickets().size() : 0;
        int totalSprints = p.getSprints() != null ? p.getSprints().size() : 0;
        int completedSprints = (int) (p.getSprints() != null
                ? p.getSprints().stream().filter(s -> s.getStatus().name().equals("COMPLETED")).count() : 0);
        int progress = totalSprints == 0 ? 0 : (completedSprints * 100 / totalSprints);

        return ProjectResponse.builder()
                .id(p.getId())
                .projectKey(p.getProjectKey())
                .name(p.getName())
                .description(p.getDescription())
                .emoji(p.getEmoji())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .priority(p.getPriority() != null ? p.getPriority().name() : null)
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .owner(p.getOwner() != null ? ticketService.mapUser(p.getOwner()) : null)
                .members(members)
                .totalTickets(totalTickets)
                .totalSprints(totalSprints)
                .progressPercent(progress)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
