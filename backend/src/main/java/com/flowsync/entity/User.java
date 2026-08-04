package com.flowsync.entity;

import com.flowsync.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String avatarColor;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean mfaEnabled = false;

    private String mfaSecret;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private LocalDateTime lastLoginTime;

    private LocalDateTime lastLogoutTime;

    private String department;

    private String position;

    @Builder.Default
    private Boolean passwordChanged = false;

    @Builder.Default
    private boolean firstLoginVerified = false;

    @Builder.Default
    private Boolean addedByAdmin = false;

    private String tempMfaCode;

    // ================= Relationships =================

    @OneToMany(mappedBy = "assignee")
    @Builder.Default
    private List<Ticket> assignedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "reporter")
    @Builder.Default
    private List<Ticket> reportedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "assigner")
    @Builder.Default
    private List<Ticket> assignedByMe = new ArrayList<>();

    @OneToMany(mappedBy = "owner")
    @Builder.Default
    private List<Project> ownedProjects = new ArrayList<>();

    @ManyToMany(mappedBy = "members")
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "recipient")
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "uploadedBy")
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    // ================= UserDetails =================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public boolean isPasswordChanged() {
        return passwordChanged != null && passwordChanged;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " +
                (lastName != null ? lastName : "");
    }

    public String getInitials() {
        String f = firstName != null ? firstName : "";
        String l = lastName != null ? lastName : "";

        return (f.isEmpty() ? "" : String.valueOf(f.charAt(0))) +
                (l.isEmpty() ? "" : String.valueOf(l.charAt(0)));
    }
}