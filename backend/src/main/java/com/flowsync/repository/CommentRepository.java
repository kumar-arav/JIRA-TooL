package com.flowsync.repository;

import com.flowsync.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    void deleteByAuthor_Id(Long authorId);

}