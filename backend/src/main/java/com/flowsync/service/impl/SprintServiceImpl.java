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

@Service
@RequiredArgsConstructor
@Transactional
public class SprintServiceImpl {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final TicketServiceImpl ticketService;

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
        return resp;
    }

    public List<SprintResponse> getByProject(Long projectId) {
        return sprintRepository.findByProject_IdOrderByStartDateAsc(projectId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SprintResponse getById(Long id) {
        return mapToResponse(sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id)));
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
