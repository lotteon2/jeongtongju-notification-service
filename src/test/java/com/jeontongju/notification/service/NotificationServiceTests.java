package com.jeontongju.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.enums.NotificationTypeEnum;
import com.jeontongju.notification.enums.RecipientTypeEnum;
import com.jeontongju.notification.repository.EmitterRepository;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class NotificationServiceTests {

  @Autowired public NotificationService notificationService;
  @Autowired public EmitterRepository emitterRepository;

  @Test
  @DisplayName("SSE 연결 후, 셀러에게 재고 소진 알림을 전송할 수 있다")
  void t1() {

    notificationService.subscribe(1L, MemberRoleEnum.ROLE_SELLER, "");
    notificationService.send(
        1L, RecipientTypeEnum.ROLE_SELLER, NotificationTypeEnum.OUT_OF_STOCK);

    Map<String, Object> emitters =
        emitterRepository.findAllEventCacheStartWithByEmail("zjadlspun1114@naver.com");

    // 이벤트 캐시에 저장된 정보 확인
    emitters.forEach(
        (key, event) -> {
          Notification notification = (Notification) event;
          assertThat(notification.getRecipientId()).isEqualTo(1L);
          assertThat(notification.getNotificationTypeEnum())
              .isEqualTo(NotificationTypeEnum.OUT_OF_STOCK);
        });

    Notification foundNotification = notificationService.findByNotificationId(1L);
    // DB에 저장된 정보 확인
    assertThat(foundNotification.getRecipientId()).isEqualTo(1L);
    assertThat(foundNotification.getNotificationTypeEnum())
        .isEqualTo(NotificationTypeEnum.OUT_OF_STOCK);
  }
}
