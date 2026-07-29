package com.flowsync.service.impl;

import com.flowsync.dto.request.LoginRequest;
import com.flowsync.dto.request.RegisterRequest;
import com.flowsync.dto.response.AuthResponse;
import com.flowsync.entity.User;
import com.flowsync.enums.Role;
import com.flowsync.exception.ResourceNotFoundException;
import com.flowsync.repository.UserRepository;
import com.flowsync.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final com.flowsync.service.EmailService emailService;

    public AuthResponse register(RegisterRequest req) {
        String normalizedEmail = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered: " + normalizedEmail);
        }

        boolean addedByAdmin = false;
        String adminEmail = null;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                addedByAdmin = true;
                adminEmail = auth.getName();
            }
        }

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole() != null ? req.getRole() : Role.DEVELOPER)
                .avatarColor(req.getAvatarColor() != null ? req.getAvatarColor() : "#2563EB")
                .passwordChanged(true)
                .addedByAdmin(addedByAdmin)
                .build();
        user = userRepository.save(user);

        if (addedByAdmin) {
            String subject = "Account Created on FlowSync";
            String body = "Hello " + req.getFirstName() + " " + req.getLastName() + ",\n\n" +
                    "An account has been created for you in FlowSync by the System Administrator (" + adminEmail + ").\n\n" +
                    "Here are your login credentials:\n" +
                    "Email: " + normalizedEmail + "\n" +
                    "Password: " + req.getPassword() + "\n\n" +
                    "Best regards,\n" +
                    "FlowSync Team";
            try {
                emailService.sendEmail(normalizedEmail, adminEmail, subject, body);
            } catch (Exception ignored) {}
        }

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ADMIN && (user.getAddedByAdmin() == null || !user.getAddedByAdmin())) {
            throw new IllegalArgumentException("Access Denied: Only Admin or Admin-registered users are permitted to authenticate.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Dummy password check: if password has not been changed, bypass MFA and prompt to change password directly
        if (!user.isPasswordChanged()) {
            return AuthResponse.builder()
                    .mfaRequired(false)
                    .passwordChanged(false)
                    .build();
        }

        if (req.getMfaCode() == null || req.getMfaCode().trim().isEmpty()) {
            // Generate 6-digit MFA code
            String code = String.valueOf((int) (100000 + Math.random() * 900000));
            user.setTempMfaCode(code);
            userRepository.save(user);

            // Send MFA code via system email
            try {
                emailService.sendSystemEmail(
                    user.getEmail(),
                    "IntelliSprint Verification Code",
                    "Hello " + user.getFullName() + ",\n\nYour IntelliSprint verification code is: " + code + "\n\nDo not share this code with anyone.\n\nBest regards,\nFlowSync Team"
                );
            } catch (Exception e) {
                // Ignore email failure in response but log it
            }

            // Never return the MFA code in the payload to ensure it is only sent via email and not visible on screen
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .passwordChanged(user.isPasswordChanged())
                    .mfaCode(null)
                    .build();
        }

        // Verify MFA code
        if (user.getTempMfaCode() == null || !user.getTempMfaCode().equals(req.getMfaCode())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.setTempMfaCode(null);
        user.setActive(true);
        user.setLastLoginTime(java.time.LocalDateTime.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .passwordChanged(user.isPasswordChanged())
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .initials(user.getInitials())
                        .avatarColor(user.getAvatarColor())
                        .build())
                .build();
    }
}
