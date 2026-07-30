package com.flowsync.controller;
import com.flowsync.dto.response.*;
import com.flowsync.entity.User;
import com.flowsync.repository.UserRepository;
import com.flowsync.service.impl.TicketServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
@RestController @RequestMapping("/users") @RequiredArgsConstructor
@Tag(name="Users")
public class UserController {
    private final UserRepository userRepository;
    private final TicketServiceImpl ticketService;
    private final com.flowsync.service.EmailService emailService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        List<UserResponse> users = userRepository.findAll().stream()
            .map(ticketService::mapUser).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(users));
    }
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.mapUser(user)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id, @RequestBody java.util.Map<String, String> req) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        String oldEmail = user.getEmail();
        if (req.containsKey("firstName")) user.setFirstName(req.get("firstName"));
        if (req.containsKey("lastName")) user.setLastName(req.get("lastName"));
        
        if (req.containsKey("password") && req.get("password") != null && !req.get("password").trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.get("password")));
        }

        boolean emailChanged = false;
        if (req.containsKey("email")) {
            String newEmail = req.get("email");
            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.trim().equalsIgnoreCase(oldEmail)) {
                String normalizedNew = newEmail.trim().toLowerCase();
                if (userRepository.existsByEmail(normalizedNew)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Email is already registered by another user"));
                }
                user.setEmail(normalizedNew);
                emailChanged = true;
            }
        }
        boolean passwordChanged = req.containsKey("password") && req.get("password") != null && !req.get("password").trim().isEmpty();
        userRepository.save(user);

        // Notify admins if email or password was updated
        if (emailChanged || passwordChanged) {
            try {
                List<User> admins = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == com.flowsync.enums.Role.ADMIN)
                        .collect(Collectors.toList());
                for (User adm : admins) {
                    emailService.sendSystemEmail(
                        adm.getEmail(),
                        "User Security Credentials Updated",
                        "Hello " + adm.getFullName() + ",\n\n" +
                        "This is to notify you that the user " + user.getFullName() + " (" + user.getEmail() + ") has updated their security credentials (username/password).\n\n" +
                        "All future system notifications and access info for this user will now be sent to their updated email address: " + user.getEmail() + ".\n\n" +
                        "Best regards,\nSorim Team"
                    );
                }
            } catch (Exception ignored) {}
        }

        if (emailChanged) {
            try {
                String roleText = user.getRole() == com.flowsync.enums.Role.ADMIN 
                    ? "Admin access is enabled and all administrative actions can be performed." 
                    : "Employee access is enabled.";

                emailService.sendSystemEmail(
                    user.getEmail(),
                    "Profile Email Updated - FlowSync",
                    "Hello " + user.getFullName() + ",\n\n" +
                    "Your FlowSync profile email address has been successfully updated to: " + user.getEmail() + ".\n\n" +
                    roleText + "\n\n" +
                    "Best regards,\nSorim Team"
                );
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(ApiResponse.ok(ticketService.mapUser(user)));
    }
}
