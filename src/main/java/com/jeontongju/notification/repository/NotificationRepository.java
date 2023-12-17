package com.jeontongju.notification.repository;

import com.jeontongju.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Optional<Notification> findByNotificationId(Long notificationId);
}
