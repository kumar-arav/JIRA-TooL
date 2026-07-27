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
        if (req.containsKey("firstName")) user.setFirstName(req.get("firstName"));
        if (req.containsKey("lastName")) user.setLastName(req.get("lastName"));
        if (req.containsKey("email")) user.setEmail(req.get("email"));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(ticketService.mapUser(user)));
    }
}
