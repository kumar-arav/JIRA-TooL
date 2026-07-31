package com.flowsync.entity;

import com.flowsync.enums.Role;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document(collection = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private Role role;

    private String avatarColor;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean mfaEnabled = false;

    private String mfaSecret;

    private String refreshToken;

    private java.time.LocalDateTime lastLoginTime;

    private java.time.LocalDateTime lastLogoutTime;

    private String department;
    private String position;

    @Builder.Default
    private Boolean passwordChanged = false;

    @Builder.Default
    private Boolean addedByAdmin = false;

    public boolean isPasswordChanged() {
        return passwordChanged != null && passwordChanged;
    }

    private String tempMfaCode;

    // Relationships
    @DocumentReference(lazy = true)
    @Builder.Default
    private List<Ticket> assignedTickets = new ArrayList<>();

    @DocumentReference(lazy = true)
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

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    public String getInitials() {
        String f = firstName != null ? firstName : "";
        String l = lastName != null ? lastName : "";
        return (f.isEmpty() ? "" : String.valueOf(f.charAt(0)))
             + (l.isEmpty() ? "" : String.valueOf(l.charAt(0)));
    }
}
