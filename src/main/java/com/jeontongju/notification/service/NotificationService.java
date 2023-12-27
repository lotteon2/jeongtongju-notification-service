package com.jeontongju.notification.service;

import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.dto.response.NotificationInfoForInquiryResponseDto;
import com.jeontongju.notification.dto.response.NotificationInfoForSingleInquiryDto;
import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import com.jeontongju.notification.exception.NotificationNotFoundException;
import com.jeontongju.notification.feign.AuthenticationClientService;
import com.jeontongju.notification.mapper.NotificationMapper;
import com.jeontongju.notification.repository.EmitterRepository;
import com.jeontongju.notification.repository.NotificationRepository;
import com.jeontongju.notification.utils.CustomErrMessage;
import io.github.bitbox.bitbox.dto.ServerErrorForNotificationDto;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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

  // SSE 연결 지속 시간 설정
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

  /**
   * SSE 연결 생성 및 유지
   *
   * @param memberId 로그인 한 회원의 식별자
   * @param memberRole 로그인 한 회원의 역할
   * @param lastEventId 마지막으로 받은 이벤트 식별자
   * @return {SseEmitter} SSE 연결 객체
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
   * 전송 못한 이벤트 확인
   *
   * @param lastEventId 마지막으로 받은 이벤트 식별자
   * @return {boolean} 전송 못한 이벤트 유무
   */
  private boolean hasLostData(String lastEventId) {
    return !lastEventId.isEmpty();
  }

  /**
   * 전송 못한 이벤트 재전송
   *
   * @param lastEventId 마지막으로 받은 이벤트 식별자
   * @param username 로그인 한 회원의 아이디(이메일)
   * @param emitterId SseEmitter를 식별자(이메일_시각)
   * @param emitter 연결된 SseEmitter 객체
   */
  private void sendLostData(
      String lastEventId, String username, String emitterId, SseEmitter emitter) {

    Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithByEmail(username);
    eventCaches.entrySet().stream()
        .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
        .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, entry.getValue()));
  }

  /**
   * 알림 저장 -> 이벤트 캐시에 저장 -> 알림 전송
   *
   * @param recipientId 수신 회원 식별자
   * @param recipientTypeEnum 수신 회원 역할
   * @param notificationTypeEnum 알림 유형
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

    String recipientEmail =
        authenticationClientService.getMemberEmailForKey(recipientId).getEmail();
    // 이벤트 캐시를 위한 키 생성
    String eventId = makeTimeIncludedId(recipientEmail);

    // 연결된 SseEmitter 가져오기
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
   * 이메일과 시간 정보가 포함된 id 생성 (이벤트 및 SseEmitter 식별자)
   *
   * @param email prefix로 사용될 이메일
   * @return {String} 생성된 식별자(이메일_시각)
   */
  private String makeTimeIncludedId(String email) {
    return email + "_" + System.currentTimeMillis();
  }

  /**
   * 실제 알림 전송
   *
   * @param emitter 연결된 SseEmitter 객체
   * @param eventId 이벤트 식별자 (이메일_시각)
   * @param emitterId SseEmitter 객체 식별자 (이메일_시각)
   * @param data 전송 내용
   */
  private void sendNotification(SseEmitter emitter, String eventId, String emitterId, Object data) {
    try {
      emitter.send(SseEmitter.event().id(eventId).name("sse").data(data));
    } catch (IOException e) {
      emitterRepository.deletedById(emitterId);
    }
  }

  @Transactional
  public void sendError(ServerErrorForNotificationDto serverErrorNotificationDto) {
    // 서버 오류로 인한 주문 과정에서의 에러 발생 시, 주문 내역 저장 로직 작성
  }

  /**
   * 알림 조회 + 안읽은 알림 개수
   *
   * @param memberId 로그인 한 회원의 식별자
   * @param memberRole 로그인 한 회원의 역할
   * @return {NotificationInfoForInquiryResponseDto} 조회할 알림 정보
   */
  public NotificationInfoForInquiryResponseDto getNotificationInfosForInquiry(
      Long memberId, MemberRoleEnum memberRole) {

    List<Notification> foundNotifications = notificationRepository.findByRecipientId(memberId);
    int notReadCounts = getUnreadCounts(foundNotifications);
    List<NotificationInfoForSingleInquiryDto> notificationDtos =
        notificationMapper.toListLookupDto(foundNotifications);
    return notificationMapper.toInquiryDto(notReadCounts, notificationDtos);
  }

  /**
   * 안 읽은 알림 개수 세기
   *
   * @param notifications 로그인 한 회원의 모든 알림
   * @return {int} 안 읽은 알림 개수
   */
  private int getUnreadCounts(List<Notification> notifications) {

    int counts = 0;
    for (Notification notification : notifications) {

      if (!notification.getIsRead()) {
        counts += 1;
      }
    }
    return counts;
  }

  /**
   * 단일 알림 읽음 처리
   *
   * @param notificationId 읽음 처리할 Notification 객체 식별자
   */
  @Transactional
  public void readNotification(Long notificationId) {

    Notification foundNotification = getNotification(notificationId);
    foundNotification.assignIsRead(true);
  }

  /**
   * 해당 회원 알림 전체 읽음 처리
   *
   * @param memberId 로그인 한 회원의 식별자
   */
  @Transactional
  public void readAllNotification(Long memberId) {

    List<Notification> foundNotifications = notificationRepository.findByRecipientId(memberId);
    for (Notification notification : foundNotifications) {
      notification.assignIsRead(true);
    }
  }

  /**
   * notificationId로 해당 알림 조회 (공통화)
   *
   * @param notificationId 읽음 처리할 Notification 객체 식별자
   * @return {Notification} Notification 객체
   */
  public Notification getNotification(Long notificationId) {

    return notificationRepository
        .findById(notificationId)
        .orElseThrow(
            () -> new NotificationNotFoundException(CustomErrMessage.NOT_FOUND_NOTIFICATION));
  }
}
