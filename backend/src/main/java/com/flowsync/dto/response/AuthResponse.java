package com.flowsync.dto.response;
import lombok.*;
@Data @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private boolean mfaRequired;
    private boolean passwordChanged;
    private String mfaCode;
    private UserSummary user;
    @Data @Builder
    public static class UserSummary {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String initials;
        private String avatarColor;
    }
}
