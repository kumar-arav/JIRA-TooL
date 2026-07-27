package com.flowsync.dto.request;
import com.flowsync.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class RegisterRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    @NotBlank @Size(min=8) private String password;
    private Role role;
    private String avatarColor;
}
