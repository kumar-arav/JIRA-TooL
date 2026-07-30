package com.flowsync.service.impl;

import com.flowsync.dto.request.CreateSprintRequest;
import com.flowsync.dto.response.SprintResponse;
import com.flowsync.entity.Sprint;
import com.flowsync.enums.SprintStatus;
import com.flowsync.enums.TicketStatus;
import com.flowsync.exception.ResourceNotFoundException;
import com.flowsync.repository.ProjectRepository;
import com.flowsync.repository.SprintRepository;
import com.flowsync.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.flowsync.service.EmailService;
import com.flowsync.repository.UserRepository;
import com.flowsync.entity.User;

@Service
@RequiredArgsConstructor
@Transactional
public class SprintServiceImpl {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final TicketServiceImpl ticketService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public SprintResponse create(CreateSprintRequest req) {
        var project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", req.getProjectId()));

        Sprint sprint = Sprint.builder()
                .name(req.getName())
                .goal(req.getGoal())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .capacityPoints(req.getCapacityPoints() != null ? req.getCapacityPoints() : 40)
                .project(project)
                .build();

        SprintResponse resp = mapToResponse(sprintRepository.save(sprint));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"SPRINT_UPDATED\"}");
        return resp;
    }

    public SprintResponse startSprint(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
        sprint.setStatus(SprintStatus.ACTIVE);
        SprintResponse resp = mapToResponse(sprintRepository.save(sprint));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"SPRINT_UPDATED\"}");
        sendSprintEmail(sprint, "started");
        return resp;
    }

    public SprintResponse completeSprint(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
        int pts = ticketRepository.sumCompletedPoints(id);
        sprint.setCompletedPoints(pts);
        sprint.setStatus(SprintStatus.COMPLETED);
        SprintResponse resp = mapToResponse(sprintRepository.save(sprint));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"SPRINT_UPDATED\"}");
        sendSprintEmail(sprint, "completed");
        return resp;
    }

    public void deleteSprint(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
        if (sprint.getTickets() != null) {
            for (com.flowsync.entity.Ticket t : sprint.getTickets()) {
                t.setSprint(null);
                ticketRepository.save(t);
            }
        }
        sprintRepository.delete(sprint);
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"SPRINT_UPDATED\"}");
    }

    private void sendSprintEmail(Sprint sprint, String action) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String updaterName = "the project administrator";
        String updaterEmail = null;
        if (auth != null && auth.getPrincipal() instanceof User) {
            User currentUser = (User) auth.getPrincipal();
            updaterName = currentUser.getFullName() + " (" + currentUser.getRole().name().replace("_", " ") + ")";
            updaterEmail = currentUser.getEmail();
        }

        List<User> managers = userRepository.findByRole(com.flowsync.enums.Role.MANAGER);
        for (User manager : managers) {
            String subject = "Sprint " + (action.equals("started") ? "Started" : "Completed") + ": " + sprint.getName();
            String body = "Hello " + manager.getFullName() + ",\n\n" +
                          "The sprint '" + sprint.getName() + "' for project '" + sprint.getProject().getName() + "' has been " + action + " by " + updaterName + ".\n\n" +
                          "Best regards,\n" +
                          "FlowSync Team";
            try {
                emailService.sendEmail(manager.getEmail(), updaterEmail, subject, body);
            } catch (Exception e) {
                // Log but do not fail the request if email cannot be sent
            }
        }
    }

    public List<SprintResponse> getByProject(Long projectId) {
        return sprintRepository.findByProject_IdOrderByStartDateAsc(projectId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SprintResponse getById(Long id) {
        return mapToResponse(sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id)));
    }

    public SprintResponse updateSprint(Long id, CreateSprintRequest req) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
        sprint.setName(req.getName());
        sprint.setGoal(req.getGoal());
        sprint.setStartDate(req.getStartDate());
        sprint.setEndDate(req.getEndDate());
        if (req.getCapacityPoints() != null) {
            sprint.setCapacityPoints(req.getCapacityPoints());
        }
        SprintResponse resp = mapToResponse(sprintRepository.save(sprint));
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"SPRINT_UPDATED\"}");
        return resp;
    }

    private SprintResponse mapToResponse(Sprint s) {
        long total    = s.getTickets() != null ? s.getTickets().size() : 0;
        long closed   = s.getTickets() != null ? s.getTickets().stream().filter(t -> t.getStatus() == TicketStatus.CLOSED).count() : 0;
        long active   = s.getTickets() != null ? s.getTickets().stream().filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS).count() : 0;

        return SprintResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .goal(s.getGoal())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .capacityPoints(s.getCapacityPoints())
                .completedPoints(s.getCompletedPoints())
                .status(s.getStatus().name())
                .progressPercent(s.getProgressPercent())
                .totalTickets(total)
                .closedTickets(closed)
                .inProgressTickets(active)
                .tickets(s.getTickets() != null
                        ? s.getTickets().stream().map(ticketService::mapToResponse).collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
