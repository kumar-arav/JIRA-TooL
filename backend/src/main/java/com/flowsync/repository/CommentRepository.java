package com.flowsync.repository;
import com.flowsync.entity.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByTicket_IdOrderByCreatedAtAsc(String ticketId);
    void deleteByAuthor_Id(String authorId);
}
