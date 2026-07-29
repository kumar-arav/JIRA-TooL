package com.flowsync.entity;

import com.flowsync.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
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

    @Column(name = "avatar_color", length = 10)
    private String avatarColor;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "mfa_enabled")
    @Builder.Default
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "last_login_time")
    private java.time.LocalDateTime lastLoginTime;

    @Column(name = "last_logout_time")
    private java.time.LocalDateTime lastLogoutTime;

    private String department;
    private String position;

    @Column(name = "password_changed")
    @Builder.Default
    private Boolean passwordChanged = false;

    @Column(name = "added_by_admin")
    @Builder.Default
    private Boolean addedByAdmin = false;

    public boolean isPasswordChanged() {
        return passwordChanged != null && passwordChanged;
    }

    @Column(name = "temp_mfa_code")
    private String tempMfaCode;

    // Relationships
    @OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ticket> assignedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ticket> reportedTickets = new ArrayList<>();

    // UserDetails impl
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return active; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }

    public String getFullName()    { return firstName + " " + lastName; }
    public String getInitials()    {
        return (firstName.isEmpty() ? "" : String.valueOf(firstName.charAt(0)))
             + (lastName.isEmpty()  ? "" : String.valueOf(lastName.charAt(0)));
    }
}
