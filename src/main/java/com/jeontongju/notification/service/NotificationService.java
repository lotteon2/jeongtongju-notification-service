package com.jeontongju.notification.service;

import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import com.jeontongju.notification.feign.AuthenticationClientService;
import com.jeontongju.notification.kafka.NotificationProducer;
import com.jeontongju.notification.mapper.NotificationMapper;
import com.jeontongju.notification.repository.EmitterRepository;
import com.jeontongju.notification.repository.NotificationRepository;
import com.jeontongju.notification.utils.CustomErrMessage;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import java.io.IOException;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@Transactional(readOnly = true)
public class NotificationService {

  private final EmitterRepository emitterRepository;
  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final AuthenticationClientService authenticationClientService;

  private final NotificationProducer notificationProducer;

  // SSE 연결 지속시간 설정
  private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

  public NotificationService(
      EmitterRepository emitterRepository,
      NotificationRepository notificationRepository,
      NotificationMapper notificationMapper,
      AuthenticationClientService authenticationClientService,
      NotificationProducer notificationProducer) {

    this.emitterRepository = emitterRepository;
    this.notificationRepository = notificationRepository;
    this.notificationMapper = notificationMapper;
    this.authenticationClientService = authenticationClientService;
    this.notificationProducer = notificationProducer;
  }

  /**
   * SSE 연결 생성 및 유지
   *
   * @param memberId
   * @param memberRole
   * @param lastEventId
   * @return SseEmitter
   */
  public SseEmitter subscribe(Long memberId, MemberRoleEnum memberRole, String lastEventId) {

    MemberEmailForKeyDto memberEmailDto =
        authenticationClientService.getMemberEmailForKey(memberId);
    String username = memberEmailDto.getEmail();

    // SseEmitter 객체 생성 및 저장
    String emitterId = makeTimeIncludedId(username);
    SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));
    emitter.onCompletion(() -> emitterRepository.deletedById(emitterId)); // SseEmitter 완료
    emitter.onTimeout(() -> emitterRepository.deletedById(emitterId)); // SseEmitter 타임아웃

    String eventId = makeTimeIncludedId(username);
    // 연결이 생성되었을 시, 확인용 더미 이벤트 전송
    sendNotification(emitter, eventId, emitterId, "EventStream Created. [email=" + username + "]");

    // 미수신 이벤트 전송
    if (hasLostData(lastEventId)) {
      sendLostData(lastEventId, username, emitterId, emitter);
    }

    return emitter;
  }

  /**
   * 전송되지 못한 이벤트 확인
   *
   * @param lastEventId
   * @return
   */
  private boolean hasLostData(String lastEventId) {
    return !lastEventId.isEmpty();
  }

  /**
   * 전송되지 못한 이벤트 재전송
   *
   * @param lastEventId
   * @param username
   * @param emitterId
   * @param emitter
   */
  private void sendLostData(
      String lastEventId, String username, String emitterId, SseEmitter emitter) {

    Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByEmail(username);
    eventCaches.entrySet().stream()
        .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
        .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, entry.getValue()));
  }

  /**
   * 알림 전솓
   *
   * @param emitter
   * @param eventId
   * @param emitterId
   * @param data
   */
  private void sendNotification(SseEmitter emitter, String eventId, String emitterId, Object data) {
    try {
      emitter.send(SseEmitter.event().id(eventId).name("sse").data(data));
    } catch (IOException e) {
      emitterRepository.deletedById(emitterId);
    }
  }

  /**
   * 이메일과 시간정보가 포함된 id 생성
   *
   * @param email
   * @return
   */
  private String makeTimeIncludedId(String email) {
    return email + "_" + System.currentTimeMillis();
  }

  /**
   * 알림 저장 -> 이벤트 캐시에 저장 -> 알림 전송
   *
   * @param recipientId
   * @param recipientTypeEnum
   * @param notificationTypeEnum
   */
  @Transactional
  public void send(
      Long recipientId,
      RecipientTypeEnum recipientTypeEnum,
      NotificationTypeEnum notificationTypeEnum) {

    // 해당 알림 저장
    Notification savedNotification =
        notificationRepository.save(
            notificationMapper.toEntity(recipientId, recipientTypeEnum, notificationTypeEnum));

    // 이벤트 캐시를 위한 키 생성
    String recipientEmail =
        authenticationClientService.getMemberEmailForKey(recipientId).getEmail();
    String eventId = recipientEmail + "_" + System.currentTimeMillis();

    Map<String, SseEmitter> emitters =
        emitterRepository.findAllEmitterStartWithByEmail(recipientEmail);

    emitters.forEach(
        (key, emitter) -> {
          // 이벤트 캐시에 저장
          emitterRepository.saveEventCache(key, savedNotification);
          // 알림 전송
          sendNotification(emitter, eventId, key, savedNotification);
        });
  }

  /**
   * notificationId로 해당 알림 조히
   *
   * @param notificationId
   * @return Notification
   */
  public Notification findByNotificationId(Long notificationId) {
    return notificationRepository
        .findByNotificationId(notificationId)
        .orElseThrow(() -> new EntityNotFoundException(CustomErrMessage.NOT_FOUND_NOTIFICATION));
  }
}
