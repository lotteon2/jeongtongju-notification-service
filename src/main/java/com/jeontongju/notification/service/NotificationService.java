package com.jeontongju.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.dto.FCMTokenDto;
import com.jeontongju.notification.dto.request.FCMNotificationRequestDto;
import com.jeontongju.notification.dto.response.*;
import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import com.jeontongju.notification.exception.NotificationNotFoundException;
import com.jeontongju.notification.feign.AuthenticationClientService;
import com.jeontongju.notification.feign.ConsumerClientService;
import com.jeontongju.notification.kafka.NotificationProducer;
import com.jeontongju.notification.mapper.NotificationMapper;
import com.jeontongju.notification.repository.EmitterRepository;
import com.jeontongju.notification.repository.NotificationRepository;
import com.jeontongju.notification.utils.CustomErrMessage;
import com.jeontongju.notification.utils.UrlEncoderManager;
import io.github.bitbox.bitbox.dto.ConsumerOrderListResponseDto;
import io.github.bitbox.bitbox.dto.MemberInfoForNotificationDto;
import io.github.bitbox.bitbox.dto.ServerErrorForNotificationDto;
import io.github.bitbox.bitbox.enums.MemberRoleEnum;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
  private final ConsumerClientService consumerClientService;
  private final RedisTemplate<String, String> redisTemplate;
  private final NotificationProducer notificationProducer;
  private final UrlEncoderManager urlEncoderManager;
  private final FCMNotificationService fcmNotificationService;

  // SSE 연결 지속 시간 설정
  private static final Long DEFAULT_TIMEOUT = 60L * 1000;

  public NotificationService(
      EmitterRepository emitterRepository,
      NotificationRepository notificationRepository,
      NotificationMapper notificationMapper,
      AuthenticationClientService authenticationClientService,
      ConsumerClientService consumerClientService,
      RedisTemplate<String, String> redisTemplate,
      NotificationProducer notificationProducer,
      UrlEncoderManager urlEncoderManager,
      FCMNotificationService fcmNotificationService) {

    this.emitterRepository = emitterRepository;
    this.notificationRepository = notificationRepository;
    this.notificationMapper = notificationMapper;
    this.authenticationClientService = authenticationClientService;
    this.consumerClientService = consumerClientService;
    this.redisTemplate = redisTemplate;
    this.notificationProducer = notificationProducer;
    this.urlEncoderManager = urlEncoderManager;
    this.fcmNotificationService = fcmNotificationService;
  }

  /**
   * SSE 연결 생성 및 유지
   *
   * @param memberId 로그인 한 회원의 식별자
   * @param lastEventId 마지막으로 받은 이벤트 식별자
   * @return {SseEmitter} SSE 연결 객체
   */
  //  @Transactional
  public SseEmitter subscribe(Long memberId, String lastEventId) {

    MemberEmailForKeyDto memberEmailDto =
        authenticationClientService.getMemberEmailForKey(memberId);
    String username = memberEmailDto.getEmail();

    // SseEmitter 객체 생성 및 저장
    String emitterId = makeTimeIncludedId(username, memberId);

    SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));
    emitter.onCompletion(() -> emitterRepository.deletedById(emitterId)); // SseEmitter 완료
    emitter.onError((e) -> emitterRepository.deletedById(emitterId));
    emitter.onTimeout(() -> emitterRepository.deletedById(emitterId)); // SseEmitter 타임 아웃

    String eventId = makeTimeIncludedId(username, memberId);
    // 연결이 생성되었을 시, 확인용 더미 이벤트 전송
    log.info("[NotificationService's subscribe's executes]: 연결 생성");
    sendNotification(
        emitter,
        eventId,
        emitterId,
        "connect",
        NotificationInfoResponseDto.builder()
            .data("EventStream Created. [email=" + username + "]")
            .build());

    // 미수신 이벤트 전송
    //    if (hasLostData(lastEventId)) {
    //      sendLostData(lastEventId, username, memberId, emitterId, emitter);
    //    }

    // 읽지 않은 이벤트 전송
    List<Notification> unreadEvents = getUnreadEvents(memberId);
    if (!unreadEvents.isEmpty()) {

      for (Notification unreadEvent : unreadEvents) {
        sendNotification(
            emitter,
            eventId,
            emitterId,
            "connect",
            NotificationInfoResponseDto.builder()
                .notificationId(unreadEvent.getNotificationId())
                .redirectUrl(unreadEvent.getRedirectLink())
                .data(unreadEvent.getNotificationTypeEnum().name())
                .build());
      }
    }
    return emitter;
  }

  /**
   * 읽지 않은 알림 가져오기
   *
   * @param memberId 로그인 한 회원 식별자
   * @return {List<Notification>} 읽지 않은 알림 객체
   */
  private List<Notification> getUnreadEvents(Long memberId) {

    List<Notification> foundUnreadNotifications =
        notificationRepository.findByRecipientIdAndIsRead(memberId, false);
    return foundUnreadNotifications;
  }

  /**
   * 전송 못한 이벤트 확인
   *
   * @param lastEventId 마지막으로 받은 이벤트 식별자
   * @return {boolean} 전송 못한 이벤트 유무
   */
  private boolean hasLostData(String lastEventId) {

    log.info("[lastEventId remains]: " + lastEventId);
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
      String lastEventId, String username, Long memberId, String emitterId, SseEmitter emitter) {

    log.info(
        "[NotificationService's sendLostData executes]: "
            + username
            + " "
            + memberId
            + " "
            + lastEventId);
    Map<String, Object> eventCaches =
        emitterRepository.findAllEventCacheStartWithByEmail(username + "_" + memberId);
    eventCaches.entrySet().stream()
        .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
        .forEach(
            entry -> {
              sendNotification(
                  emitter,
                  entry.getKey(),
                  emitterId,
                  "connect",
                  notificationMapper.toNotificationDto(-1L, null, entry.getValue()));
              log.info("[In eventCaches]: " + emitterId + " " + "send");
            });
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
    String eventId = makeTimeIncludedId(recipientEmail, recipientId);

    // 연결된 SseEmitter 가져오기
    Map<String, SseEmitter> emitters =
        emitterRepository.findAllEmitterStartWithByEmail(recipientEmail + "_" + recipientId);

    emitters.forEach(
        (key, emitter) -> {
          // 이벤트 캐시에 저장
          // emitterRepository.saveEventCache(key, savedNotification);
          // 알림 전송
          log.info("이벤트 알림 전송");
          sendNotification(
              emitter,
              eventId,
              key,
              "happy",
              notificationMapper.toNotificationDto(
                  savedNotification.getNotificationId(),
                  savedNotification.getRedirectLink(),
                  savedNotification.getNotificationTypeEnum().name()));
        });
  }

  /**
   * 이메일과 시간 정보가 포함된 id 생성 (이벤트 및 SseEmitter 식별자)
   *
   * @param email prefix로 사용될 이메일
   * @param memberId prefix로 사용될 회원 식별자
   * @return {String} 생성된 식별자(이메일_식별자_시각)
   */
  public String makeTimeIncludedId(String email, Long memberId) {
    return email + "_" + memberId + "_" + System.currentTimeMillis();
  }

  /**
   * 실제 알림 전송
   *
   * @param emitter 연결된 SseEmitter 객체
   * @param eventId 이벤트 식별자 (이메일_식별자_시각)
   * @param emitterId SseEmitter 객체 식별자 (이메일_식별자_시각)
   * @param data 전송 내용
   */
  private void sendNotification(
      SseEmitter emitter,
      String eventId,
      String emitterId,
      String eventName,
      NotificationInfoResponseDto data) {
    try {
      emitter.send(SseEmitter.event().id(eventId).name(eventName).data(data));
      log.info("[NotificationService's sendNotification executes]: 알림 전송 완료: " + eventName);
    } catch (IOException e) {
      emitterRepository.deletedById(emitterId);
    }
  }

  /**
   * 서버 오류로 주문이 안 된 경우 주문 실패 알림 전송
   *
   * @param serverErrorDto 소비자 정보 + 오류난 서버 + 주문 내역
   * @throws JsonProcessingException JSON 데이터 처리 OR 파싱 과정 예외
   */
  @Transactional
  public void sendError(ServerErrorForNotificationDto serverErrorDto)
      throws JsonProcessingException {

    // 서버 오류로 인한 주문 과정에서의 에러 발생 시, 주문 내역 저장 로직 작성
    ValueOperations<String, String> stringStringValueOperations = redisTemplate.opsForValue();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // redis key 생성
    Long consumerId = serverErrorDto.getRecipientId();
    String redisKey = "CONSUMER_" + consumerId;

    // 오류난 주문 내역 가져오기
    ConsumerOrderListResponseDto fakeOrder = serverErrorDto.getError().createFakeOrder();
    String stringFakeOrder = objectMapper.writeValueAsString(fakeOrder);

    // redis에 오류난 주문 내역 저장
    stringStringValueOperations.set(redisKey, stringFakeOrder);

    Notification savedNotification =
        notificationRepository.save(
            notificationMapper.toIncludedRedirectLinkEntity(
                consumerId,
                RecipientTypeEnum.ROLE_CONSUMER,
                serverErrorDto.getNotificationType(),
                "https://jeontongju.shop/orderdetail"));

    MemberEmailForKeyDto memberEmailForKey =
        authenticationClientService.getMemberEmailForKey(consumerId);
    Map<String, SseEmitter> emitters =
        emitterRepository.findAllEmitterStartWithByEmail(
            memberEmailForKey.getEmail() + "_" + consumerId);

    // event id 생성
    String eventId = makeTimeIncludedId(memberEmailForKey.getEmail(), consumerId);
    String redirectUrl =
        savedNotification.getRedirectLink()
            + "/"
            + fakeOrder.getOrder().getOrdersId()
            + "?order="
            + URLEncoder.encode(stringFakeOrder, StandardCharsets.UTF_8);

    emitters.forEach(
        (key, emitter) -> {
          sendNotification(
              emitter,
              eventId,
              key,
              "happy",
              NotificationInfoResponseDto.builder()
                  .notificationId(savedNotification.getNotificationId())
                  .redirectUrl(stringFakeOrder)
                  .data(serverErrorDto.getNotificationType().name())
                  .build());
        });

//    log.info("[try getting fcm token]");
//    FCMTokenDto fcmTokenDto = consumerClientService.getConsumerFCMToken(consumerId);
//    String fcmToken = fcmTokenDto.getFcmToken();
//
//    if (fcmToken != null) {
//
//      log.info("[fcmToken]: " + fcmToken);
//      fcmNotificationService.sendNotificationByToken(
//          consumerId,
//          FCMNotificationRequestDto.builder()
//              .title("[전통주점.] 주문실패 - Server Error!")
//              .body("죄송합니다. 서버오류로 인해 주문 실패했습니다.")
//              .build());
//    }
  }

  /**
   * 서버 오류로 주문 취소가 안 된 경우 주문 취소 실패 알림 전송
   *
   * @param memberInfoDto 소비자 정보 + 오류난 서버
   */
  @Transactional
  public void sendCancelingServerError(MemberInfoForNotificationDto memberInfoDto) {

    Long recipientId = memberInfoDto.getRecipientId();

    Notification savedNotification =
        notificationRepository.save(
            notificationMapper.toIncludedRedirectLinkEntity(
                recipientId,
                memberInfoDto.getRecipientType(),
                memberInfoDto.getNotificationType(),
                ""));

    MemberEmailForKeyDto memberEmailForKey =
        authenticationClientService.getMemberEmailForKey(recipientId);
    Map<String, SseEmitter> emitters =
        emitterRepository.findAllEmitterStartWithByEmail(
            memberEmailForKey.getEmail() + "_" + recipientId);

    String eventId = makeTimeIncludedId(memberEmailForKey.getEmail(), recipientId);

    emitters.forEach(
        (key, emitter) -> {
          sendNotification(
              emitter,
              eventId,
              key,
              "happy",
              notificationMapper.toNotificationDto(
                  savedNotification.getNotificationId(),
                  null,
                  memberInfoDto.getNotificationType().name()));
        });
  }

  /**
   * 알림 클릭 시, 읽음 처리와 함께 Redirect 링크 반환
   *
   * @param memberId 로그인 한 회원 식별자
   * @param notificationId 읽음 처리할 알림 식별자
   * @return {UrlForRedirectResponseDto} 해당 알림의 Redirect Link 정보
   */
  @Transactional
  public UrlForRedirectResponseDto getRedirectLink(Long memberId, Long notificationId)
      throws JsonProcessingException {

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    readNotification(notificationId);

    Notification foundNotification = getNotification(notificationId);

    if (foundNotification.getRedirectLink() == null) {
      throw new RuntimeException(CustomErrMessage.NOT_FOUND_REDIRECT_LINK);
    }

    ValueOperations<String, String> stringStringValueOperations = redisTemplate.opsForValue();

    String redisValue = stringStringValueOperations.get("CONSUMER_" + memberId);

    NotificationTypeEnum notificationTypeEnum = foundNotification.getNotificationTypeEnum();
    if (notificationTypeEnum == NotificationTypeEnum.OUT_OF_STOCK
        || notificationTypeEnum == NotificationTypeEnum.BALANCE_ACCOUNTS
        || notificationTypeEnum == NotificationTypeEnum.SUCCESS_SUBSCRIPTION_PAYMENTS) {

      if (redisValue == null) {

        return notificationMapper.toRedirectUrlDto(foundNotification.getRedirectLink());
      }
    }

    ConsumerOrderListResponseDto consumerOrderListResponseDto =
        objectMapper.readValue(redisValue, ConsumerOrderListResponseDto.class);

    String ordersId = consumerOrderListResponseDto.getOrder().getOrdersId();

    return notificationMapper.toRedirectUrlDto(
        foundNotification.getRedirectLink()
            + "/"
            + ordersId
            + "?order="
            + urlEncoderManager.encodeURIComponent(redisValue));
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

  public void testNotificationProduce(Long memberId, MemberRoleEnum memberRole) {

    RecipientTypeEnum recipientType =
        memberRole.equals(MemberRoleEnum.ROLE_CONSUMER)
            ? RecipientTypeEnum.ROLE_CONSUMER
            : RecipientTypeEnum.ROLE_SELLER;

    notificationProducer.send(
        KafkaTopicNameInfo.SEND_NOTIFICATION,
        MemberInfoForNotificationDto.builder()
            .recipientId(memberId)
            .recipientType(recipientType)
            .notificationType(NotificationTypeEnum.OUT_OF_STOCK)
            .build());
  }

  public List<EmitterInfoForSingleInquiryDto> getEmitters(
      Long memberId, MemberRoleEnum memberRole) {

    String email = authenticationClientService.getMemberEmailForKey(memberId).getEmail();
    Map<String, SseEmitter> allEmitterStartWithByEmail =
        emitterRepository.findAllEmitterStartWithByEmail(email + "_" + memberId);

    List<EmitterInfoForSingleInquiryDto> emitters = new ArrayList<>();
    for (String key : allEmitterStartWithByEmail.keySet()) {
      if (key.startsWith(email + "_" + memberId)) {
        SseEmitter sseEmitter = allEmitterStartWithByEmail.get(key);
        EmitterInfoForSingleInquiryDto build =
            EmitterInfoForSingleInquiryDto.builder().emitterId(key).sseEmitter(sseEmitter).build();
        emitters.add(build);
      }
    }
    return emitters;
  }

  public void resetEmitters(Long memberId) {

    String memberEmail = authenticationClientService.getConsumerEmail(memberId);
    log.info("[memberEmail]: " + memberEmail);
    emitterRepository.delete(memberEmail, memberId);
  }
}
