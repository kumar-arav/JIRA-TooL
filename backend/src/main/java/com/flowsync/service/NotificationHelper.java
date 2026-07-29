package com.flowsync.service;

import com.flowsync.entity.Notification;
import com.flowsync.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationHelper {
    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNotificationSafe(Notification notification) {
        notificationRepository.save(notification);
    }
}
