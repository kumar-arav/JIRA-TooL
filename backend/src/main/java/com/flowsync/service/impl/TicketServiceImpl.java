package com.flowsync.service.impl;

import com.flowsync.dto.request.CommentRequest;
import com.flowsync.dto.request.CreateTicketRequest;
import com.flowsync.dto.request.UpdateTicketStatusRequest;
import com.flowsync.dto.response.*;
import com.flowsync.entity.*;
import com.flowsync.enums.TicketStatus;
import com.flowsync.exception.ResourceNotFoundException;
import com.flowsync.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TicketServiceImpl {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final com.flowsync.service.EmailService emailService;
    private final com.flowsync.service.NotificationHelper notificationHelper;

    public TicketResponse createTicket(CreateTicketRequest req, Long reporterId) {
        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", req.getProjectId()));

        long count = ticketRepository.findByProject_Id(req.getProjectId()).size() + 1;
        String ticketKey = project.getProjectKey() + "-" + count;

        Ticket ticket = Ticket.builder()
                .ticketKey(ticketKey)
                .title(req.getTitle())
                .description(req.getDescription())
                .storyPoints(req.getStoryPoints())
                .priority(req.getPriority())
                .status(TicketStatus.TODO)
                .project(project)
                .build();

        if (reporterId != null) {
            User reporter = userRepository.findById(reporterId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));
            if (reporter.getRole() != com.flowsync.enums.Role.ADMIN) {
                boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(reporter.getId());
                boolean isMember = project.getMembers().stream().anyMatch(m -> m.getId().equals(reporter.getId()) || m.getEmail().equalsIgnoreCase(reporter.getEmail()));
                if (!isOwner && !isMember) {
                    throw new org.springframework.security.access.AccessDeniedException("You are not authorized to create tickets in this project. You must be added to the project members first.");
                }
            }
            ticket.setReporter(reporter);
        }

        if (req.getSprintId() != null) {
            ticket.setSprint(sprintRepository.findById(req.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint", req.getSprintId())));
        }

        if (req.getAssigneeId() != null) {
            ticket.setAssignee(userRepository.findById(req.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.getAssigneeId())));
        }

        Ticket saved = ticketRepository.save(ticket);
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\", \"ticketId\": " + saved.getId() + "}");
        return mapToResponse(saved);
    }

    public TicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest req) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        // Sprint validation for developers/testers
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            if (currentUser.getRole() == com.flowsync.enums.Role.DEVELOPER || currentUser.getRole() == com.flowsync.enums.Role.TESTER) {
                if (ticket.getSprint() == null || ticket.getSprint().getStatus() != com.flowsync.enums.SprintStatus.ACTIVE) {
                    throw new IllegalArgumentException("Developers and Testers can only work on tickets in active sprints");
                }
            }
        }

        // Closure validation
        if (req.getStatus() == TicketStatus.CLOSED) {
            if (!ticket.isTesterApproved() || !ticket.isManagerApproved()) {
                throw new IllegalArgumentException("Ticket cannot be closed without tester and manager approval");
            }
            if (req.getClosureNotes() == null || req.getClosureNotes().isBlank()) {
                throw new IllegalArgumentException("Closure notes are required");
            }
            ticket.setClosureNotes(req.getClosureNotes());
            ticket.setClosureProofUrl(req.getClosureProofUrl());
        }

        ticket.setStatus(req.getStatus());
        TicketResponse res = mapToResponse(ticketRepository.save(ticket));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\", \"ticketId\": " + ticketId + ", \"status\": \"" + req.getStatus() + "\"}");
        return res;
    }

    public TicketResponse updateAssignee(Long ticketId, Long assigneeId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        User newAssignee = null;
        if (assigneeId != null) {
            newAssignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", assigneeId));
        }
        ticket.setAssignee(newAssignee);
        TicketResponse res = mapToResponse(ticketRepository.save(ticket));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\", \"ticketId\": " + ticketId + "}");

        if (newAssignee != null) {
            String assignedByName = "the system administrator";
            String assignedByEmail = null;
            try {
                // Get currently logged-in user who made the change
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof User) {
                    User currentUser = (User) auth.getPrincipal();
                    assignedByName = currentUser.getFullName() + " (" + currentUser.getRole().name().replace("_", " ") + ")";
                    assignedByEmail = currentUser.getEmail();
                }

                String ticketUrl = "http://localhost:3000/tickets/" + ticket.getId();
                emailService.sendEmail(
                    newAssignee.getEmail(),
                    assignedByEmail,
                    "Ticket Assigned: " + ticket.getTicketKey() + " - " + ticket.getTitle(),
                    "Hello " + newAssignee.getFullName() + ",\n\n" +
                    "The ticket '" + ticket.getTitle() + "' (" + ticket.getTicketKey() + ") has been assigned/transferred to you by " + assignedByName + ".\n\n" +
                    "You can view the ticket here: " + ticketUrl + "\n\n" +
                    "Kindly update and complete it as needed.\n\n" +
                    "Best regards,\n" +
                    "Sorim Team"
                );
            } catch (Exception e) {
                log.error("Failed to send email to assigned user: {}", e.getMessage());
            }

            // Also notify all admins
            try {
                List<User> admins = userRepository.findByRole(com.flowsync.enums.Role.ADMIN);
                for (User adm : admins) {
                    if (!adm.getEmail().equalsIgnoreCase(newAssignee.getEmail())) {
                        emailService.sendEmail(adm.getEmail(), assignedByEmail, "[Admin Alert] Ticket Assigned: " + ticket.getTicketKey(),
                            "Hello Administrator " + adm.getFullName() + ",\n\n" +
                            "This is to notify you that the ticket '" + ticket.getTitle() + "' (" + ticket.getTicketKey() + ") has been assigned/transferred to " + newAssignee.getFullName() + " (" + newAssignee.getEmail() + ") by " + assignedByName + ".\n\n" +
                            "Best regards,\nSorim Team"
                        );
                    }
                }
            } catch (Exception ignored) {}
        }
        return res;
    }

    public TicketResponse updateSprint(Long ticketId, Long sprintId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        com.flowsync.entity.Sprint sprint = null;
        if (sprintId != null) {
            sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint", sprintId));
        }
        ticket.setSprint(sprint);
        TicketResponse res = mapToResponse(ticketRepository.save(ticket));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\", \"ticketId\": " + ticketId + "}");
        return res;
    }

    public TicketResponse approveTester(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        ticket.setTesterApproved(true);
        TicketResponse res = mapToResponse(ticketRepository.save(ticket));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\", \"ticketId\": " + ticketId + "}");
        return res;
    }

    public TicketResponse approveManager(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        ticket.setManagerApproved(true);
        TicketResponse res = mapToResponse(ticketRepository.save(ticket));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\", \"ticketId\": " + ticketId + "}");
        return res;
    }

    public CommentResponse addComment(Long ticketId, CommentRequest req, Long authorId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", authorId));

        Comment comment = Comment.builder()
                .content(req.getContent())
                .ticket(ticket)
                .author(author)
                .build();
        comment = commentRepository.save(comment);
        return mapCommentToResponse(comment);
    }

    public List<TicketResponse> getByProject(Long projectId) {
        return ticketRepository.findByProject_Id(projectId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TicketResponse> getBySprint(Long sprintId) {
        return ticketRepository.findBySprint_Id(sprintId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TicketResponse> getMyTickets(Long userId) {
        return ticketRepository.findByAssignee_Id(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public TicketResponse getById(Long id) {
        return mapToResponse(ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id)));
    }

    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        ticketRepository.delete(ticket);
        try {
            com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\"}");
        } catch (Exception ignored) {}
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    public TicketResponse mapToResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .ticketKey(t.getTicketKey())
                .title(t.getTitle())
                .description(t.getDescription())
                .storyPoints(t.getStoryPoints())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .priority(t.getPriority() != null ? t.getPriority().name() : null)
                .dueDate(t.getDueDate())
                .assignee(t.getAssignee() != null ? mapUser(t.getAssignee()) : null)
                .assigner(t.getAssigner() != null ? mapUser(t.getAssigner()) : null)
                .reporter(t.getReporter() != null ? mapUser(t.getReporter()) : null)
                .projectName(t.getProject() != null ? t.getProject().getName() : null)
                .projectKey(t.getProject() != null ? t.getProject().getProjectKey() : null)
                .sprintName(t.getSprint() != null ? t.getSprint().getName() : null)
                .sprintId(t.getSprint() != null ? t.getSprint().getId() : null)
                .testerApproved(t.isTesterApproved())
                .managerApproved(t.isManagerApproved())
                .closureNotes(t.getClosureNotes())
                .comments(t.getComments() != null
                        ? t.getComments().stream().map(this::mapCommentToResponse).collect(Collectors.toList())
                        : List.of())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private CommentResponse mapCommentToResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .content(c.getContent())
                .author(mapUser(c.getAuthor()))
                .createdAt(c.getCreatedAt())
                .build();
    }

    public UserResponse mapUser(User u) {
        if (u == null) return null;
        return UserResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .initials(u.getInitials())
                .avatarColor(u.getAvatarColor())
                .active(u.isActive())
                .department(u.getDepartment())
                .position(u.getPosition())
                .addedByAdmin(u.getAddedByAdmin() != null && u.getAddedByAdmin())
                .lastLoginTime(u.getLastLoginTime())
                .lastLogoutTime(u.getLastLogoutTime())
                .build();
    }

    private void createNotification(User recipient, String type, String title, String message, Long ticketId) {
        try {
            Notification n = Notification.builder()
                    .type(com.flowsync.enums.NotificationType.valueOf(type))
                    .title(title)
                    .message(message)
                    .recipient(recipient)
                    .relatedTicketId(ticketId)
                    .build();
            notificationHelper.saveNotificationSafe(n);
            com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"NOTIFICATION_RECEIVED\", \"recipientId\": " + recipient.getId() + ", \"title\": \"" + title + "\", \"message\": \"" + message + "\"}");
        } catch (Exception ignored) {}
    }
}
