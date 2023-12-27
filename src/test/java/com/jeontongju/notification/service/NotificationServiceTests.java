package com.jeontongju.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import com.jeontongju.notification.feign.AuthenticationClientService;
import com.jeontongju.notification.repository.EmitterRepository;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@Transactional
@Slf4j
public class NotificationServiceTests {

  @Autowired public NotificationService notificationService;
  @Autowired public EmitterRepository emitterRepository;
  @Autowired public AuthenticationClientService authenticationClientService;

  @Test
  @DisplayName("SSE 연결 후, 셀러에게 재고 소진 알림을 전송할 수 있다")
  void t1() {

    notificationService.subscribe(1L, "");
    notificationService.send(1L, RecipientTypeEnum.ROLE_SELLER, NotificationTypeEnum.OUT_OF_STOCK);

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

    Notification foundNotification = notificationService.getNotification(1L);
    // DB에 저장된 정보 확인
    assertThat(foundNotification.getRecipientId()).isEqualTo(1L);
    assertThat(foundNotification.getNotificationTypeEnum())
        .isEqualTo(NotificationTypeEnum.OUT_OF_STOCK);
  }

  @Test
  @DisplayName("로그인 시 SSE 연결 후, 로그인 한 회원 식별자로 SSE 연결 객체를 조회할 수 있다")
  public void 연결객체_조회하기() {

    Long loginedId = 1L;
    SseEmitter sseEmitter = notificationService.subscribe(loginedId, "");

    MemberEmailForKeyDto emailDto = authenticationClientService.getMemberEmailForKey(loginedId);

    Map<String, SseEmitter> emitters =
        emitterRepository.findAllEmitterStartWithByEmail(emailDto.getEmail() + "_" + loginedId);
    boolean isContained = emitters.containsValue(sseEmitter);
    assertThat(isContained).isEqualTo(true);
  }
}
