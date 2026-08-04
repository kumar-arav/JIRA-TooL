package com.flowsync.config;

import com.flowsync.entity.*;
import com.flowsync.enums.*;
import com.flowsync.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;
    private final SprintRepository sprintRepo;
    private final TicketRepository ticketRepo;
    private final NotificationRepository notifRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {

        log.info("========== DataSeeder Started ==========");

        // Skip seeding if data already exists
        if (userRepo.count() > 0) {
            log.info("Database already initialized. Skipping data seeding.");
            return;
        }

        log.info("Fresh database detected.");

        // -------------------------------------------------
        // Add your initial seed data here if required.
        // Example:
        //
        // User admin = save(user(
        //      "Admin",
        //      "User",
        //      "admin@flowsync.com",
        //      Role.ADMIN,
        //      "#1976d2"));
        //
        // -------------------------------------------------

        log.info("DataSeeder completed successfully.");
    }

    private User user(String fn, String ln, String email, Role role, String color) {
        return User.builder()
                .firstName(fn)
                .lastName(ln)
                .email(email)
                .password(encoder.encode("password123"))
                .role(role)
                .avatarColor(color)
                .mfaEnabled(true)
                .active(true)
                .passwordChanged(true)
                .build();
    }

    private User save(User user) {
        return userRepo.save(user);
    }

    private Sprint sprint(String name,
                          String goal,
                          LocalDate start,
                          LocalDate end,
                          int cap,
                          int done,
                          SprintStatus status,
                          Project project) {

        return sprintRepo.save(
                Sprint.builder()
                        .name(name)
                        .goal(goal)
                        .startDate(start)
                        .endDate(end)
                        .capacityPoints(cap)
                        .completedPoints(done)
                        .status(status)
                        .project(project)
                        .build());
    }

    private void ticket(String key,
                        String title,
                        String desc,
                        int pts,
                        Priority priority,
                        TicketStatus status,
                        Project project,
                        Sprint sprint,
                        User assignee,
                        User assigner,
                        User reporter,
                        LocalDate due) {

        Ticket ticket = Ticket.builder()
                .ticketKey(key)
                .title(title)
                .description(desc)
                .storyPoints(pts)
                .priority(priority)
                .status(status)
                .project(project)
                .sprint(sprint)
                .assignee(assignee)
                .assigner(assigner)
                .reporter(reporter)
                .dueDate(due)
                .testerApproved(status == TicketStatus.CLOSED)
                .managerApproved(status == TicketStatus.CLOSED)
                .closureNotes(status == TicketStatus.CLOSED
                        ? "Feature completed and tested successfully."
                        : null)
                .build();

        ticketRepo.save(ticket);
    }

    private void notif(User recipient,
                       String type,
                       String title,
                       String message,
                       Long ticketId) {

        notifRepo.save(
                Notification.builder()
                        .type(NotificationType.valueOf(type))
                        .title(title)
                        .message(message)
                        .recipient(recipient)
                        .relatedTicketId(ticketId)
                        .build());
    }
}