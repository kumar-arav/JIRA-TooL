package com.flowsync.controller;

import com.flowsync.dto.response.ApiResponse;
import com.flowsync.dto.response.UserResponse;
import com.flowsync.entity.User;
import com.flowsync.enums.Role;
import com.flowsync.repository.UserRepository;
import com.flowsync.service.EmailService;
import com.flowsync.service.impl.TicketServiceImpl;
import com.flowsync.repository.ProjectRepository;
import com.flowsync.repository.TicketRepository;
import com.flowsync.repository.CommentRepository;
import com.flowsync.repository.NotificationRepository;
import com.flowsync.entity.Project;
import com.flowsync.entity.Ticket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Operations", description = "Endpoints restricted to System Administrators")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TicketServiceImpl ticketService;
    private final ProjectRepository projectRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;

    @PostMapping("/add-employee")
    @Operation(summary = "Admin adds a new employee to the system")
    public ResponseEntity<ApiResponse<UserResponse>> addEmployee(@RequestBody Map<String, String> req) {
        String name = req.get("name");
        String email = req.get("email");
        String department = req.get("department");
        String position = req.get("position");

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Name and Email are required"));
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is already registered"));
        }

        // Split name into first and last name
        String firstName = name;
        String lastName = "";
        int lastSpaceIdx = name.lastIndexOf(' ');
        if (lastSpaceIdx > 0) {
            firstName = name.substring(0, lastSpaceIdx).trim();
            lastName = name.substring(lastSpaceIdx).trim();
        } else {
            lastName = "Employee";
        }

        // Map position to a system Role enum
        Role role = Role.DEVELOPER;
        String roleStr = req.get("role");
        if (roleStr != null && !roleStr.trim().isEmpty()) {
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (Exception ignored) {}
        } else {
            String posLower = position != null ? position.toLowerCase() : "";
            if (posLower.contains("admin")) {
                role = Role.ADMIN;
            } else if (posLower.contains("scrum") || posLower.contains("scrum master")) {
                role = Role.SCRUM_MASTER;
            } else if (posLower.contains("owner")) {
                role = Role.PROJECT_OWNER;
            } else if (posLower.contains("cto") || posLower.contains("chief technology")) {
                role = Role.CTO;
            } else if (posLower.contains("vp") || posLower.contains("president")) {
                role = Role.VP;
            } else if (posLower.contains("manager")) {
                role = Role.MANAGER;
            } else if (posLower.contains("tester") || posLower.contains("qa")) {
                role = Role.TESTER;
            } else if (posLower.contains("trainee")) {
                role = Role.TRAINEE;
            }
        }

        // Generate temporary password
        int randomNum = (int) (10000 + Math.random() * 90000);
        String tempPassword = "EMP-" + randomNum;

        User employee = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(normalizedEmail)
                .password(passwordEncoder.encode(tempPassword))
                .role(role)
                .avatarColor("#1E40AF") // Default blue avatar
                .department(department)
                .position(position)
                .active(true)
                .mfaEnabled(true)
                .passwordChanged(false) // Must change password on first login
                .addedByAdmin(true)
                .build();

        User savedEmployee = userRepository.save(employee);

        // Get current admin's email to send from their address
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = authentication != null ? authentication.getName() : null;

        // Send Welcome Email
        String subject = "Welcome to FlowSync - Your Temporary Credentials";
        String body = "Hello " + name + ",\n\n" +
                "You have been registered as an employee in FlowSync with the role: " + role.name() + " under the " + department + " department.\n\n" +
                "Here are your temporary login details:\n" +
                "Email: " + email + "\n" +
                "Temporary Password: " + tempPassword + "\n\n" +
                "You will be prompted to change this password during your first login.\n\n" +
                "Best regards,\n" +
                "FlowSync Team";

        try {
            emailService.sendEmail(email, adminEmail, subject, body);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", email, e.getMessage());
        }

        // Broadcast user addition via websocket
        try {
            com.flowsync.config.WebSocketConfiguration.broadcast(
                    "{\"type\": \"USER_REGISTERED\", \"user\": \"" + email + "\", \"role\": \"" + role.name() + "\"}"
            );
        } catch (Exception ignored) {}

        UserResponse res = ticketService.mapUser(savedEmployee);
        return ResponseEntity.ok(ApiResponse.ok("Employee registered successfully", res));
    }

    @PutMapping("/update-email")
    @Operation(summary = "Admin updates their own email address")
    public ResponseEntity<ApiResponse<UserResponse>> updateEmail(@RequestBody Map<String, String> req) {
        String newEmail = req.get("email");
        if (newEmail == null || newEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
        }
        String normalizedNew = newEmail.trim().toLowerCase();

        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentAdminEmail = authentication.getName();

        User admin = userRepository.findByEmail(currentAdminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        if (!currentAdminEmail.equalsIgnoreCase(normalizedNew) && userRepository.existsByEmail(normalizedNew)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is already registered by another user"));
        }

        admin.setEmail(normalizedNew);
        User savedAdmin = userRepository.save(admin);

        // Update credentials file if password is not changed
        if (!savedAdmin.isPasswordChanged()) {
            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of("admin_credentials.txt"),
                    "Admin Email: " + newEmail + "\nTemporary Password: (Retained previous temp password)\n"
                );
            } catch (Exception ignored) {}
        }

        UserResponse res = ticketService.mapUser(savedAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Admin email updated successfully", res));
    }

    @DeleteMapping("/delete-employee/{id}")
    @Operation(summary = "Admin deletes an employee account")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        User employee = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (employee.getRole() == Role.ADMIN) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Admin user cannot be deleted"));
        }

        // 1. Remove user from project members and reset project owner
        List<Project> projects = projectRepository.findAll();
        for (Project p : projects) {
            if (p.getMembers().contains(employee)) {
                p.getMembers().remove(employee);
                projectRepository.save(p);
            }
            if (employee.equals(p.getOwner())) {
                p.setOwner(null);
                projectRepository.save(p);
            }
        }

        // 2. Remove/nullify assignee, assigner, reporter on tickets
        List<Ticket> tickets = ticketRepository.findAll();
        for (Ticket t : tickets) {
            boolean modified = false;
            if (employee.equals(t.getAssignee())) {
                t.setAssignee(null);
                modified = true;
            }
            if (employee.equals(t.getAssigner())) {
                t.setAssigner(null);
                modified = true;
            }
            if (employee.equals(t.getReporter())) {
                t.setReporter(null);
                modified = true;
            }
            if (modified) {
                ticketRepository.save(t);
            }
        }

        // 3. Delete comments authored by the user
        commentRepository.deleteByAuthor_Id(id);

        // 4. Delete notifications sent to the user
        notificationRepository.deleteByRecipient_Id(id);

        userRepository.delete(employee);

        try {
            com.flowsync.config.WebSocketConfiguration.broadcast(
                    "{\"type\": \"USER_REGISTERED\", \"user\": \"" + employee.getEmail() + "\", \"action\": \"DELETED\"}"
            );
        } catch (Exception ignored) {}

        return ResponseEntity.ok(ApiResponse.ok("Employee deleted successfully", null));
    }
}
