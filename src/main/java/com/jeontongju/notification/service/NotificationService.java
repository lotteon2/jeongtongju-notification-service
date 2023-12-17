package com.jeontongju.notification.service;

import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import com.jeontongju.notification.enums.NotificationTypeEnum;
import com.jeontongju.notification.enums.RecipientTypeEnum;
import com.jeontongju.notification.feign.AuthenticationClientService;
import com.jeontongju.notification.mapper.NotificationMapper;
import com.jeontongju.notification.repository.EmitterRepository;
import com.jeontongju.notification.repository.NotificationRepository;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class NotificationService {

  private final EmitterRepository emitterRepository;
  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final AuthenticationClientService authenticationClientService;

  // SSE 연결 지속시간 설정
  private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

  public NotificationService(
      EmitterRepository emitterRepository,
      NotificationRepository notificationRepository,
      NotificationMapper notificationMapper,
      AuthenticationClientService authenticationClientService) {
    this.emitterRepository = emitterRepository;
    this.notificationRepository = notificationRepository;
    this.notificationMapper = notificationMapper;
    this.authenticationClientService = authenticationClientService;
  }

  public SseEmitter subscribe(Long memberId, MemberRoleEnum memberRole, String lastEventId) {

    MemberEmailForKeyDto memberEmailDto =
        authenticationClientService.getMemberEmailForKey(memberId);
    String username = memberEmailDto.getEmail();
    log.info("username: " + username);

    // SseEmitter 객체 생성 및 저장
    String emitterId = makeTimeIncludedId(username);
    SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));
    emitter.onCompletion(() -> emitterRepository.deletedById(emitterId)); // SseEmitter 완료
    emitter.onTimeout(() -> emitterRepository.deletedById(emitterId)); // SseEmitter 타임아웃

    String eventId = makeTimeIncludedId(username);
    // 연결이 생성되었을 시, 더미 이벤트 전송
    sendNotification(emitter, eventId, emitterId, "EventStream Created. [email=" + username + "]");

    // 미수신 이벤트 전송
    if (hasLostData(lastEventId)) {
      sendLostData(lastEventId, username, emitterId, emitter);
    }

    return emitter;
  }

  private void sendLostData(
      String lastEventId, String username, String emitterId, SseEmitter emitter) {

    Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByEmail(username);
    eventCaches.entrySet().stream()
        .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
        .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, entry.getValue()));
  }

  private boolean hasLostData(String lastEventId) {
    return !lastEventId.isEmpty();
  }

  private void sendNotification(SseEmitter emitter, String eventId, String emitterId, Object data) {
    try {
      emitter.send(SseEmitter.event().id(eventId).name("sse").data(data));
    } catch (IOException e) {
      emitterRepository.deletedById(emitterId);
    }
  }

  private String makeTimeIncludedId(String email) {
    return email + "_" + System.currentTimeMillis();
  }

  public void send(
      Long recipientId,
      RecipientTypeEnum recipientTypeEnum,
      NotificationTypeEnum notificationTypeEnum) {

    Notification savedNotification =
        notificationRepository.save(
            notificationMapper.toEntity(recipientId, recipientTypeEnum, notificationTypeEnum));

    String recipientEmail =
        authenticationClientService.getMemberEmailForKey(recipientId).getEmail();
    String eventId = recipientEmail + "_" + System.currentTimeMillis();
    Map<String, SseEmitter> emitters =
        emitterRepository.findAllEmitterStartWithByEmail(recipientEmail);

    emitters.forEach(
        (key, emitter) -> {
          emitterRepository.saveEventCache(key, savedNotification);
          sendNotification(
              emitter,
              eventId,
              key,
              notificationMapper.toEntity(recipientId, recipientTypeEnum, notificationTypeEnum));
        });
  }
}
