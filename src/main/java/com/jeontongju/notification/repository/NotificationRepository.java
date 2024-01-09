package com.jeontongju.notification.repository;

import com.jeontongju.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Optional<Notification> findByNotificationId(Long notificationId);

  List<Notification> findByRecipientId(Long memberId);

  List<Notification> findByRecipientIdAndIsRead(Long memberId, boolean isRead);
}
