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
import java.util.List;

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
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        boolean firstTimeSeed = userRepo.count() == 0;

        if (firstTimeSeed) {
            log.info("First boot detected — seeding Admin user...");
            try {
                mongoTemplate.dropCollection(DatabaseSequence.class);
                projectRepo.deleteAll();
                sprintRepo.deleteAll();
                ticketRepo.deleteAll();
                notifRepo.deleteAll();
                log.info("Dropped database sequences and collections for fresh seed starting at ID 1.");
            } catch (Exception e) {
                log.error("Failed to clean collections: {}", e.getMessage());
            }

            String adminRawPassword = "admin123";

            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of("admin_credentials.txt"),
                    "Admin Email: admin@flowsync.com\nTemporary Password: " + adminRawPassword + "\n"
                );
                log.info("Admin temporary credentials written to admin_credentials.txt");
            } catch (Exception e) {
                log.error("Failed to write admin credentials file: {}", e.getMessage());
            }

            User admin = User.builder()
                    .firstName("Admin").lastName("User")
                    .email("admin@flowsync.com")
                    .password(encoder.encode(adminRawPassword))
                    .role(Role.ADMIN).avatarColor("#374151")
                    .mfaEnabled(true).active(true).passwordChanged(false)
                    .build();
            save(admin);

            log.info("Admin user seeded. All data will persist across restarts.");
        } else {
            log.info("Database already initialised — all existing data retained.");
        }
    }

    private User user(String fn, String ln, String email, Role role, String color) {
        return User.builder().firstName(fn).lastName(ln).email(email)
                .password(encoder.encode("password123")).role(role).avatarColor(color).mfaEnabled(true).active(true)
                .passwordChanged(true).build();
    }

    private User save(User u) { return userRepo.save(u); }

    private Sprint sprint(String name, String goal, LocalDate start, LocalDate end,
                          int cap, int done, SprintStatus status, Project project) {
        return sprintRepo.save(Sprint.builder()
                .name(name).goal(goal).startDate(start).endDate(end)
                .capacityPoints(cap).completedPoints(done).status(status).project(project).build());
    }

    private void ticket(String key, String title, String desc, int pts, Priority priority,
                        TicketStatus status, Project project, Sprint sprint,
                        User assignee, User assigner, User reporter, LocalDate due) {
        Ticket t = Ticket.builder()
                .ticketKey(key).title(title).description(desc).storyPoints(pts)
                .priority(priority).status(status).project(project).sprint(sprint)
                .assignee(assignee).assigner(assigner).reporter(reporter).dueDate(due)
                .testerApproved(status == TicketStatus.CLOSED)
                .managerApproved(status == TicketStatus.CLOSED)
                .closureNotes(status == TicketStatus.CLOSED ? "Feature completed and tested successfully." : null)
                .build();
        ticketRepo.save(t);
    }

    private void notif(User recipient, String type, String title, String message, Long ticketId) {
        notifRepo.save(Notification.builder()
                .type(NotificationType.valueOf(type))
                .title(title).message(message)
                .recipient(recipient).relatedTicketId(ticketId).build());
    }
}
