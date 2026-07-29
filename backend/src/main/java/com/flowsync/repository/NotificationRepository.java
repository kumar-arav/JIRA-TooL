package com.flowsync.repository;
import com.flowsync.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByRecipient_IdAndReadFalse(Long userId);
    long countByRecipient_IdAndReadFalse(Long userId);
    void deleteByRecipient_Id(Long userId);
}
