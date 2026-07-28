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

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;
        log.info("Seeding FlowSync demo data…");
        User sarah  = save(user("Sarah",  "Chen",   "sarah.chen@flowsync.com",  Role.SCRUM_MASTER,    "#1E40AF"));
        User james  = save(user("James",  "Doe",    "james.doe@flowsync.com",   Role.DEVELOPER,       "#059669"));
        User ana    = save(user("Ana",    "Lima",   "ana.lima@flowsync.com",    Role.DEVELOPER,       "#7C3AED"));
        User mike   = save(user("Mike",   "Kim",    "mike.kim@flowsync.com",    Role.DEVELOPER,       "#0D9488"));
        User priya  = save(user("Priya",  "Rao",    "priya.rao@flowsync.com",   Role.TESTER,          "#D97706"));
        User tom    = save(user("Tom",    "Marsh",  "tom.marsh@flowsync.com",   Role.DEVELOPER,       "#DC2626"));
        User kevin  = save(user("Kevin",  "Wu",     "kevin.wu@flowsync.com",    Role.CTO,             "#7C3AED"));
        User rita   = save(user("Rita",   "Patel",  "rita.patel@flowsync.com",  Role.MANAGER,         "#DB2777"));
        User admin  = save(user("Admin",  "User",   "admin@flowsync.com",       Role.ADMIN,           "#374151"));
        User olivia = save(user("Olivia", "Grant",  "olivia.grant@flowsync.com",Role.PROJECT_OWNER,   "#0EA5E9"));
        User victor = save(user("Victor", "Pace",   "victor.pace@flowsync.com", Role.VP,              "#8B5CF6"));
        User dan    = save(user("Dan",    "Okafor", "dan.okafor@flowsync.com",  Role.TRAINEE,         "#94A3B8"));

        log.info("Created {} users", userRepo.count());
        log.info("IntelliSprint initial users seeded. Starting with a clean slate for projects, sprints, and tickets.");
    }

    private User user(String fn, String ln, String email, Role role, String color) {
        return User.builder().firstName(fn).lastName(ln).email(email)
                .password(encoder.encode("password123")).role(role).avatarColor(color).mfaEnabled(true).active(false).build();
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
