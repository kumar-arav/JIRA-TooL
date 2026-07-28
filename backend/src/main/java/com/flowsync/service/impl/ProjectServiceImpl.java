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

import com.flowsync.service.EmailService;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketServiceImpl ticketService;
    private final EmailService emailService;

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
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            if (currentUser.getRole() == com.flowsync.enums.Role.ADMIN ||
                currentUser.getRole() == com.flowsync.enums.Role.CTO ||
                currentUser.getRole() == com.flowsync.enums.Role.VP) {
                return projectRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
            } else {
                return projectRepository.findAll().stream()
                    .filter(p -> (p.getOwner() != null && p.getOwner().getId().equals(currentUser.getId())) ||
                                 p.getMembers().stream().anyMatch(m -> m.getId().equals(currentUser.getId())))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            }
        }
        return projectRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ProjectResponse getById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            if (currentUser.getRole() != com.flowsync.enums.Role.ADMIN &&
                currentUser.getRole() != com.flowsync.enums.Role.CTO &&
                currentUser.getRole() != com.flowsync.enums.Role.VP) {
                boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(currentUser.getId());
                boolean isMember = project.getMembers().stream().anyMatch(m -> m.getId().equals(currentUser.getId()));
                if (!isOwner && !isMember) {
                    throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this project");
                }
            }
        }
        return mapToResponse(project);
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

            // Identify who performed the add action
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String addedByName = "the project administrator";
            String addedByEmail = null;
            if (auth != null && auth.getPrincipal() instanceof User) {
                User currentUser = (User) auth.getPrincipal();
                addedByName = currentUser.getFullName() + " (" + currentUser.getRole().name().replace("_", " ") + ")";
                addedByEmail = currentUser.getEmail();
            }

            // Send notification email
            String projectUrl = "http://localhost:3000/projects/" + project.getId();
            String subject = "Added to project: " + project.getName();
            String body = "Hello " + user.getFullName() + ",\n\n" +
                          "You have been added to the project '" + project.getName() + "' (" + project.getProjectKey() + ") by " + addedByName + ".\n\n" +
                          "You can access the project here: " + projectUrl + "\n\n" +
                          "You will be able to view and manage contents only for the projects you are part of.\n\n" +
                          "Best regards,\n" +
                          "FlowSync Team";
            emailService.sendEmail(user.getEmail(), addedByEmail, subject, body);
        }
    }

    public void removeMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (project.getMembers().contains(user)) {
            project.getMembers().remove(user);
            projectRepository.save(project);
            com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");

            // Identify who performed the remove action
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String removedByName = "the project administrator";
            String removedByEmail = null;
            if (auth != null && auth.getPrincipal() instanceof User) {
                User currentUser = (User) auth.getPrincipal();
                removedByName = currentUser.getFullName() + " (" + currentUser.getRole().name().replace("_", " ") + ")";
                removedByEmail = currentUser.getEmail();
            }

            // Send notification email
            String subject = "Removed from project: " + project.getName();
            String body = "Hello " + user.getFullName() + ",\n\n" +
                          "You have been removed from the project '" + project.getName() + "' (" + project.getProjectKey() + ") by " + removedByName + ".\n\n" +
                          "Best regards,\n" +
                          "FlowSync Team";
            emailService.sendEmail(user.getEmail(), removedByEmail, subject, body);
        }
    }

    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        projectRepository.delete(project);
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");
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
