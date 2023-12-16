package com.jeontongju.notification.domain;

import com.jeontongju.notification.enums.NotificationTypeEnum;
import com.jeontongju.notification.enums.RecipientTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

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

  @Column(name = "recipient_type", nullable = false)
  private RecipientTypeEnum recipientTypeEnum;

  @Column(name = "notification_type", nullable = false)
  private NotificationTypeEnum notificationTypeEnum;
}
