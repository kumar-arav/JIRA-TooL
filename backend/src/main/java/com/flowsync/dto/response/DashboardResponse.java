package com.flowsync.dto.response;
import lombok.*;
import java.util.List;
@Data @Builder
public class DashboardResponse {
    private int totalProjects;
    private int activeSprints;
    private int openTickets;
    private int teamVelocity;
    private double portfolioHealth;
    private double onTimeDelivery;
    private double defectRate;
    private int teamUtilization;
    private SprintResponse activeSprint;
    private List<ProjectResponse> recentProjects;
}
