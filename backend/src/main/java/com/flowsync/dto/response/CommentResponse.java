package com.flowsync.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder
public class CommentResponse {
    private Long id;
    private String content;
    private UserResponse author;
    private LocalDateTime createdAt;
}
