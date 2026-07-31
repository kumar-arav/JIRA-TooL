package com.flowsync.repository;
import com.flowsync.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface CommentRepository extends MongoRepository<Comment, Long> {
    List<Comment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
    void deleteByAuthor_Id(Long authorId);
}
