package com.jeontongju.notification.domain;

import static javax.persistence.GenerationType.IDENTITY;

import com.jeontongju.notification.enums.NotificationTypeEnum;
import com.jeontongju.notification.enums.RecipientTypeEnum;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Notification {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "notification_id")
  private Long notificationId;

  @Column(name = "recipient_id", nullable = false)
  private Long recipientId;

  @Enumerated(EnumType.STRING)
  @Column(name = "recipient_type", nullable = false)
  private RecipientTypeEnum recipientTypeEnum;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false)
  private NotificationTypeEnum notificationTypeEnum;

  @Column(name = "is_read", nullable = false)
  @Builder.Default
  private Boolean isRead = false;
}
