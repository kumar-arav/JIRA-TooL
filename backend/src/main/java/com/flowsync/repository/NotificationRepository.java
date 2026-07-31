package com.flowsync.repository;
import com.flowsync.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(String userId);
    List<Notification> findByRecipient_IdAndReadFalse(String userId);
    long countByRecipient_IdAndReadFalse(String userId);
    void deleteByRecipient_Id(String userId);
}
