package com.flowsync.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String role;
    private String initials;
    private String avatarColor;
    private boolean active;
    private int taskCount;
    private int utilizationPercent;
    private LocalDateTime lastLoginTime;
    private LocalDateTime lastLogoutTime;
    private LocalDateTime createdAt;
}
