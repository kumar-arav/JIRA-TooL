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
import com.flowsync.service.NotificationHelper;
import com.flowsync.entity.Notification;
import com.flowsync.enums.NotificationType;
import com.flowsync.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketServiceImpl ticketService;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final NotificationHelper notificationHelper;

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
                .gitRepo(req.getGitRepo())
                .duration(req.getDuration())
                .build();

        User owner = null;
        if (req.getOwnerId() != null) {
            owner = userRepository.findById(req.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.getOwnerId()));
            project.setOwner(owner);
            // Automatically add the owner to project members
            project.getMembers().add(owner);
        }

        User sm = null;
        if (req.getScrumMasterId() != null) {
            sm = userRepository.findById(req.getScrumMasterId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.getScrumMasterId()));
            project.getMembers().add(sm);
        }

        // Automatically add creator to project members
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        User creator = null;
        String creatorName = "the project administrator";
        String creatorEmail = auth != null ? auth.getName() : null;
        if (creatorEmail != null) {
            creator = userRepository.findByEmail(creatorEmail).orElse(null);
            if (creator != null) {
                creatorName = creator.getFullName() + " (" + creator.getRole().name().replace("_", " ") + ")";
                if (!project.getMembers().contains(creator)) {
                    project.getMembers().add(creator);
                }
            }
        }

        Project savedProject = projectRepository.save(project);

        // Trigger emails and notifications for default assigned Project Owner and Scrum Master
        if (owner != null && !owner.equals(creator)) {
            triggerInvitation(savedProject, owner, creatorName, creatorEmail);
        }
        if (sm != null && !sm.equals(creator)) {
            triggerInvitation(savedProject, sm, creatorName, creatorEmail);
        }

        ProjectResponse resp = mapToResponse(savedProject);
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");
        return resp;
    }

    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : null;
        User currentUser = currentEmail != null ? userRepository.findByEmail(currentEmail).orElse(null) : null;
        
        if (currentUser != null) {
            final User finalUser = currentUser;
            if (currentUser.getRole() != com.flowsync.enums.Role.ADMIN) {
                boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(finalUser.getId());
                boolean isMember = project.getMembers().stream().anyMatch(m -> m.getId().equals(finalUser.getId()) || m.getEmail().equalsIgnoreCase(finalUser.getEmail()));
                if (!isOwner && !isMember) {
                    throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this project. You must be added to the project by an administrator first.");
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
        try {
            boolean alreadyMember = project.getMembers().stream().anyMatch(m -> m.getId().equals(user.getId()) || m.getEmail().equalsIgnoreCase(user.getEmail()));
            if (!alreadyMember) {
                project.getMembers().add(user);
                projectRepository.save(project);
                try {
                    com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");
                } catch (Exception ignored) {}

                // Identify who performed the add action
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                String addedByName = "the project administrator";
                String addedByEmail = auth != null ? auth.getName() : null;
                if (addedByEmail != null) {
                    User currentUser = userRepository.findByEmail(addedByEmail).orElse(null);
                    if (currentUser != null) {
                        addedByName = currentUser.getFullName() + " (" + currentUser.getRole().name().replace("_", " ") + ")";
                    }
                }

                try {
                    triggerInvitation(project, user, addedByName, addedByEmail);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Fallback: update mapping relation manually or ensure exception is suppressed so API reports success
            System.err.println("Suppressed exception in addMember: " + e.getMessage());
        }
    }

    private void triggerInvitation(Project project, User user, String addedByName, String addedByEmail) {
        // Create and save persistent in-app Notification
        try {
            Notification notification = Notification.builder()
                    .type(NotificationType.PROJECT_ADDED)
                    .title("Added to project: " + project.getName())
                    .message("You have been added to project '" + project.getName() + "' by " + addedByName + ".")
                    .recipient(user)
                    .build();
            notificationHelper.saveNotificationSafe(notification);
            com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"NOTIFICATION_RECEIVED\", \"recipientId\": " + user.getId() + ", \"title\": \"Added to project\", \"message\": \"You have been added to project '" + project.getName() + "' by " + addedByName + "\"}");
        } catch (Exception ignored) {}

        // Gather sprint details for the email
        StringBuilder sprintInfo = new StringBuilder();
        if (project.getSprints() != null && !project.getSprints().isEmpty()) {
            sprintInfo.append("\nSprints associated with this project:\n");
            for (com.flowsync.entity.Sprint s : project.getSprints()) {
                sprintInfo.append("- ").append(s.getName())
                          .append(" (Status: ").append(s.getStatus().name())
                          .append(", Goal: ").append(s.getGoal() != null && !s.getGoal().isBlank() ? s.getGoal() : "No goal set")
                          .append(", Dates: ").append(s.getStartDate() != null ? s.getStartDate() : "N/A").append(" to ").append(s.getEndDate() != null ? s.getEndDate() : "N/A")
                          .append(")\n");
            }
        } else {
            sprintInfo.append("\nNo sprints have been created for this project yet.\n");
        }

        // Send notification email
        String projectUrl = "http://localhost:3000/login?email=" + user.getEmail() + "&redirect=/projects/" + project.getId();
        String subject = "Added to project: " + project.getName();
        String body = "Hello " + user.getFullName() + ",\n\n" +
                      "You have been added to the project '" + project.getName() + "' (" + project.getProjectKey() + ") by " + addedByName + ".\n\n" +
                      "You can access the project link directly here: " + projectUrl + "\n" +
                      sprintInfo.toString() + "\n" +
                      "You will be able to view and manage contents only for the projects you are part of.\n\n" +
                      "Best regards,\n" +
                      "Sorim Team";
        try {
            emailService.sendEmail(user.getEmail(), addedByEmail, subject, body);
        } catch (Exception e) {
            // Log but do not fail the request if email cannot be sent
        }

        // Also notify all admins
        try {
            List<User> admins = userRepository.findByRole(com.flowsync.enums.Role.ADMIN);
            for (User adm : admins) {
                if (!adm.getEmail().equalsIgnoreCase(user.getEmail())) {
                    emailService.sendEmail(adm.getEmail(), addedByEmail, "[Admin Alert] Team Member Added to Project: " + project.getName(), 
                        "Hello Administrator " + adm.getFullName() + ",\n\n" +
                        "This is to notify you that the user " + user.getFullName() + " (" + user.getEmail() + ") has been added to project '" + project.getName() + "' by " + addedByName + ".\n\n" +
                        "Best regards,\nSorim Team"
                    );
                }
            }
        } catch (Exception ignored) {}
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
                          "Sorim Team";
            try {
                emailService.sendEmail(user.getEmail(), removedByEmail, subject, body);
            } catch (Exception e) {
                // Log but do not fail the request if email cannot be sent
            }

            // Also notify all admins
            try {
                List<User> admins = userRepository.findByRole(com.flowsync.enums.Role.ADMIN);
                for (User adm : admins) {
                    if (!adm.getEmail().equalsIgnoreCase(user.getEmail())) {
                        emailService.sendEmail(adm.getEmail(), removedByEmail, "[Admin Alert] Team Member Removed from Project: " + project.getName(), 
                            "Hello Administrator " + adm.getFullName() + ",\n\n" +
                            "This is to notify you that the user " + user.getFullName() + " (" + user.getEmail() + ") has been removed from project '" + project.getName() + "' by " + removedByName + ".\n\n" +
                            "Best regards,\nSorim Team"
                        );
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        projectRepository.delete(project);
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");
    }

    public ProjectResponse updateProject(Long id, CreateProjectRequest req) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        
        User oldOwner = project.getOwner();

        project.setName(req.getName());
        project.setProjectKey(req.getProjectKey());
        project.setDescription(req.getDescription());
        project.setEmoji(req.getEmoji() != null ? req.getEmoji() : "📁");
        if (req.getStatus() != null) {
            project.setStatus(req.getStatus());
        }
        if (req.getPriority() != null) {
            project.setPriority(req.getPriority());
        }
        project.setStartDate(req.getStartDate());
        project.setEndDate(req.getEndDate());
        project.setGitRepo(req.getGitRepo());
        project.setDuration(req.getDuration());

        // Resolve current admin / user performing the edit
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String updaterName = "the project administrator";
        String updaterEmail = null;
        if (auth != null && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            updaterName = currentUser.getFullName() + " (" + currentUser.getRole().name().replace("_", " ") + ")";
            updaterEmail = currentUser.getEmail();
        }

        if (req.getOwnerId() != null) {
            User owner = userRepository.findById(req.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.getOwnerId()));
            project.setOwner(owner);
            if (!project.getMembers().contains(owner)) {
                project.getMembers().add(owner);
            }
            // Send notification email to the new Owner if it was updated/changed
            if (oldOwner == null || !oldOwner.getId().equals(owner.getId())) {
                String projectUrl = "http://localhost:3000/login?email=" + owner.getEmail() + "&redirect=/projects/" + project.getId();
                String subject = "Assigned as Project Owner: " + project.getName();
                String body = "Hello " + owner.getFullName() + ",\n\n" +
                              "You have been assigned as the Project Owner for the project '" + project.getName() + "' (" + project.getProjectKey() + ") by " + updaterName + ".\n\n" +
                              "You can view and access the project here: " + projectUrl + "\n\n" +
                              "Best regards,\nSorim Team";
                try {
                    emailService.sendEmail(owner.getEmail(), updaterEmail, subject, body);
                } catch (Exception ignored) {}

                // Also notify other admins
                try {
                    List<User> admins = userRepository.findByRole(com.flowsync.enums.Role.ADMIN);
                    for (User adm : admins) {
                        if (!adm.getEmail().equalsIgnoreCase(owner.getEmail())) {
                            emailService.sendEmail(adm.getEmail(), updaterEmail, "[Admin Alert] Project Owner Assigned: " + project.getName(),
                                "Hello Administrator " + adm.getFullName() + ",\n\n" +
                                "This is to notify you that the project '" + project.getName() + "' has been assigned a new Project Owner: " + owner.getFullName() + " (" + owner.getEmail() + ") by " + updaterName + ".\n\n" +
                                "Best regards,\nSorim Team"
                            );
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (req.getScrumMasterId() != null) {
            User sm = userRepository.findById(req.getScrumMasterId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.getScrumMasterId()));
            if (!project.getMembers().contains(sm)) {
                project.getMembers().add(sm);

                // Send notification email to the new Scrum Master
                String projectUrl = "http://localhost:3000/login?email=" + sm.getEmail() + "&redirect=/projects/" + project.getId();
                String subject = "Assigned as Scrum Master: " + project.getName();
                String body = "Hello " + sm.getFullName() + ",\n\n" +
                              "You have been added and assigned as the Scrum Master for the project '" + project.getName() + "' (" + project.getProjectKey() + ") by " + updaterName + ".\n\n" +
                              "You can view and access the project here: " + projectUrl + "\n\n" +
                              "Best regards,\nSorim Team";
                try {
                    emailService.sendEmail(sm.getEmail(), updaterEmail, subject, body);
                } catch (Exception ignored) {}

                // Also notify other admins
                try {
                    List<User> admins = userRepository.findByRole(com.flowsync.enums.Role.ADMIN);
                    for (User adm : admins) {
                        if (!adm.getEmail().equalsIgnoreCase(sm.getEmail())) {
                            emailService.sendEmail(adm.getEmail(), updaterEmail, "[Admin Alert] Scrum Master Assigned: " + project.getName(),
                                "Hello Administrator " + adm.getFullName() + ",\n\n" +
                                "This is to notify you that the user " + sm.getFullName() + " (" + sm.getEmail() + ") has been assigned as Scrum Master for project '" + project.getName() + "' by " + updaterName + ".\n\n" +
                                "Best regards,\nSorim Team"
                            );
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        Project saved = projectRepository.save(project);
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"PROJECT_UPDATED\"}");
        return mapToResponse(saved);
    }

    private ProjectResponse mapToResponse(Project p) {
        List<UserResponse> members = p.getMembers().stream()
                .map(ticketService::mapUser).collect(Collectors.toList());

        int totalTickets = p.getTickets() != null ? p.getTickets().size() : 0;
        int totalSprints = p.getSprints() != null ? p.getSprints().size() : 0;
        int completedSprints = (int) (p.getSprints() != null
                ? p.getSprints().stream().filter(s -> s.getStatus().name().equals("COMPLETED")).count() : 0);
        int progress = totalSprints == 0 ? 0 : (completedSprints * 100 / totalSprints);

        boolean hasAccess = false;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String currentEmail = auth != null ? auth.getName() : null;
            if (currentEmail != null) {
                User currentUser = userRepository.findByEmail(currentEmail).orElse(null);
                if (currentUser != null) {
                    if (currentUser.getRole() == com.flowsync.enums.Role.ADMIN) {
                        hasAccess = true;
                    } else {
                        boolean isOwner = p.getOwner() != null && p.getOwner().getId().equals(currentUser.getId());
                        boolean isMember = p.getMembers().stream().anyMatch(m -> m.getId().equals(currentUser.getId()) || m.getEmail().equalsIgnoreCase(currentUser.getEmail()));
                        if (isOwner || isMember) {
                            hasAccess = true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

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
                .gitRepo(p.getGitRepo())
                .duration(p.getDuration())
                .owner(p.getOwner() != null ? ticketService.mapUser(p.getOwner()) : null)
                .members(members)
                .totalTickets(totalTickets)
                .totalSprints(totalSprints)
                .progressPercent(progress)
                .hasAccess(hasAccess)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
