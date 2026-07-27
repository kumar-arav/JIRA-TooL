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
        String lastLoginStr = user != null && user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : java.time.LocalDateTime.now().toString();
        com.flowsync.config.WebSocketConfiguration
                .broadcast("{\"type\": \"USER_LOGIN\", \"user\": \"" + req.getEmail() + "\", \"lastLoginTime\": \"" + lastLoginStr + "\"}");
        return ResponseEntity.ok(ApiResponse.ok("Login successful", res));
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
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            user.setEmail(newEmail);
        }
        userRepository.save(user);
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
}
