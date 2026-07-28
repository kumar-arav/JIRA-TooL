package com.flowsync.controller;

import com.flowsync.dto.request.*;
import com.flowsync.dto.response.*;
import com.flowsync.service.impl.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, register, JWT")
public class AuthController {
    private final AuthServiceImpl authService;
    private final com.flowsync.repository.UserRepository userRepository;
    private final com.flowsync.service.EmailService emailService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"USER_REGISTERED\", \"user\": \""
                + req.getEmail() + "\", \"role\": \"" + req.getRole() + "\"}");
        return ResponseEntity.ok(ApiResponse.ok("User registered successfully", res));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email & password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req);
        com.flowsync.entity.User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        String lastLoginStr = user != null && user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : "";
        com.flowsync.config.WebSocketConfiguration
                .broadcast("{\"type\": \"USER_LOGIN\", \"user\": \"" + req.getEmail() + "\", \"lastLoginTime\": \"" + lastLoginStr + "\"}");
        return ResponseEntity.ok(ApiResponse.ok("Logged in successfully", res));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user by email")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody java.util.Map<String, String> req) {
        String email = req.get("email");
        com.flowsync.entity.User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            user.setActive(false);
            java.time.LocalDateTime logoutTime = java.time.LocalDateTime.now();
            user.setLastLogoutTime(logoutTime);
            userRepository.save(user);
            com.flowsync.config.WebSocketConfiguration
                    .broadcast("{\"type\": \"USER_LOGOUT\", \"user\": \"" + email + "\", \"lastLogoutTime\": \"" + logoutTime.toString() + "\"}");
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @PutMapping("/rename-by-email")
    @Operation(summary = "Rename user by email")
    public ResponseEntity<ApiResponse<Void>> renameByEmail(@RequestBody java.util.Map<String, String> req) {
        String email = req.get("email");
        String newEmail = req.get("newEmail");
        String firstName = req.get("firstName");
        String lastName = req.get("lastName");
        com.flowsync.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        boolean emailChanged = false;
        if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equalsIgnoreCase(email)) {
            user.setEmail(newEmail);
            emailChanged = true;
        }
        userRepository.save(user);

        if (emailChanged) {
            try {
                emailService.sendEmail(
                    user.getEmail(),
                    null,
                    "Profile Email Updated - FlowSync",
                    "Hello " + user.getFullName() + ",\n\n" +
                    "Your FlowSync profile email address has been successfully updated to: " + user.getEmail() + ".\n\n" +
                    "Best regards,\n" +
                    "FlowSync Team"
                );
            } catch (Exception e) {
                // Log exception silently
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("User renamed successfully", null));
    }

    @PostMapping("/send-sms")
    @Operation(summary = "Send SMS verification code")
    public ResponseEntity<ApiResponse<String>> sendSms(@RequestBody java.util.Map<String, String> req) {
        String phone = req.get("phone");
        String code = req.get("code");
        if (phone == null || code == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Phone and code are required"));
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String form = "phone=" + java.net.URLEncoder.encode(phone, java.nio.charset.StandardCharsets.UTF_8)
                    + "&message="
                    + java.net.URLEncoder.encode(
                            "Your IntelliSprint authorization code is " + code + ". Do not share this code.",
                            java.nio.charset.StandardCharsets.UTF_8)
                    + "&key=textbelt";

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://textbelt.com/text"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(form))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body != null && body.contains("\"success\":true")) {
                return ResponseEntity.ok(ApiResponse.ok("SMS dispatched successfully", body));
            } else {
                return ResponseEntity.ok(ApiResponse.ok("SMS API limit reached on free key. Code is: " + code, body));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok("Failed to connect to SMS gateway: " + e.getMessage(), "{\"success\":false,\"error\":\"Connection failed: " + e.getMessage() + "\"}"));
        }
    }

    @PostMapping("/reset-defaults")
    @Operation(summary = "Reset demo accounts to defaults")
    public ResponseEntity<ApiResponse<Void>> resetDefaults() {
        // Reset Admin
        userRepository.findByRole(com.flowsync.enums.Role.ADMIN).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Admin");
            u.setLastName("User");
            u.setEmail("admin@flowsync.com");
            userRepository.save(u);
        });

        // Reset Scrum Master
        userRepository.findByRole(com.flowsync.enums.Role.SCRUM_MASTER).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Sarah");
            u.setLastName("Chen");
            u.setEmail("sarah.chen@flowsync.com");
            userRepository.save(u);
        });

        // Reset Project Owner
        userRepository.findByRole(com.flowsync.enums.Role.PROJECT_OWNER).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Olivia");
            u.setLastName("Grant");
            u.setEmail("olivia.grant@flowsync.com");
            userRepository.save(u);
        });

        // Reset CTO
        userRepository.findByRole(com.flowsync.enums.Role.CTO).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Kevin");
            u.setLastName("Wu");
            u.setEmail("kevin.wu@flowsync.com");
            userRepository.save(u);
        });

        // Reset VP
        userRepository.findByRole(com.flowsync.enums.Role.VP).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Victor");
            u.setLastName("Pace");
            u.setEmail("victor.pace@flowsync.com");
            userRepository.save(u);
        });

        // Reset Manager
        userRepository.findByRole(com.flowsync.enums.Role.MANAGER).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Rita");
            u.setLastName("Patel");
            u.setEmail("rita.patel@flowsync.com");
            userRepository.save(u);
        });

        // Reset Tester
        userRepository.findByRole(com.flowsync.enums.Role.TESTER).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Priya");
            u.setLastName("Rao");
            u.setEmail("priya.rao@flowsync.com");
            userRepository.save(u);
        });

        // Reset Trainee
        userRepository.findByRole(com.flowsync.enums.Role.TRAINEE).stream().findFirst().ifPresent(u -> {
            u.setFirstName("Dan");
            u.setLastName("Okafor");
            u.setEmail("dan.okafor@flowsync.com");
            userRepository.save(u);
        });

        // Reset Developers
        java.util.List<com.flowsync.entity.User> devs = userRepository.findByRole(com.flowsync.enums.Role.DEVELOPER);
        if (devs.size() > 0) {
            devs.get(0).setFirstName("James");
            devs.get(0).setLastName("Doe");
            devs.get(0).setEmail("james.doe@flowsync.com");
            userRepository.save(devs.get(0));
        }
        if (devs.size() > 1) {
            devs.get(1).setFirstName("Ana");
            devs.get(1).setLastName("Lima");
            devs.get(1).setEmail("ana.lima@flowsync.com");
            userRepository.save(devs.get(1));
        }
        if (devs.size() > 2) {
            devs.get(2).setFirstName("Mike");
            devs.get(2).setLastName("Kim");
            devs.get(2).setEmail("mike.kim@flowsync.com");
            userRepository.save(devs.get(2));
        }
        if (devs.size() > 3) {
            devs.get(3).setFirstName("Tom");
            devs.get(3).setLastName("Marsh");
            devs.get(3).setEmail("tom.marsh@flowsync.com");
            userRepository.save(devs.get(3));
        }

        return ResponseEntity.ok(ApiResponse.ok("Demo credentials reset successfully", null));
    }
}
