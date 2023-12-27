package com.jeontongju.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jeontongju.notification.domain.Notification;
import com.jeontongju.notification.dto.temp.MemberEmailForKeyDto;
import com.jeontongju.notification.feign.AuthenticationClientService;
import com.jeontongju.notification.repository.EmitterRepository;
import io.github.bitbox.bitbox.dto.*;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import io.github.bitbox.bitbox.enums.PaymentTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@Transactional
@Slf4j
public class NotificationServiceTests {

  @Autowired public NotificationService notificationService;
  @Autowired public EmitterRepository emitterRepository;
  @Autowired public AuthenticationClientService authenticationClientService;
  @Autowired public RedisTemplate<String, String> redisTemplate;

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

  @Test
  @DisplayName("주문 중 서버 에러가 난 경우, 주문 실패 알림을 줘야 한다")
  public void 주문_실패() throws JsonProcessingException {

    UserPointUpdateDto pointUpdateDto =
        UserPointUpdateDto.builder().consumerId(1L).point(null).totalAmount(30000L).build();

    UserCouponUpdateDto couponUpdateDto =
        UserCouponUpdateDto.builder()
            .consumerId(1L)
            .couponCode(null)
            .couponAmount(null)
            .totalAmount(30000L)
            .build();
    List<ProductUpdateDto> productUpdateDtos = new ArrayList<>();
    ProductUpdateDto productUpdateDto =
        ProductUpdateDto.builder()
            .productId("3529215b-5588-4562-9e14-c0ebb0685667")
            .productCount(1L)
            .build();
    productUpdateDtos.add(productUpdateDto);

    List<ProductInfoDto> productInfoDtos = new ArrayList<>();
    ProductInfoDto build =
        ProductInfoDto.builder()
            .productId("3529215b-5588-4562-9e14-c0ebb0685667")
            .productName("진로 복순도가")
            .productPrice(30000L)
            .productCount(1L)
            .sellerId(4L)
            .sellerName("비트박스")
            .productImg(
                "https://jeontongju-dev-bucket.s3.ap-northeast-2.amazonaws.com/dir/66d7d69b-112e-4b76-8b34-b61ab60840e0test_img.png")
            .build();
    productInfoDtos.add(build);

    OrderCreationDto orderCreationDto =
        OrderCreationDto.builder()
            .totalPrice(30000L)
            .consumerId(1L)
            .orderDate(LocalDateTime.now())
            .orderId("e2305c0f-c6b7-416c-aa00-3b03e141484a")
            .productInfoDtoList(productInfoDtos)
            .recipientName("최성훈")
            .recipientPhoneNumber("01012345678")
            .basicAddress("서울특별시 서대문구 연희동")
            .addressDetail("101")
            .zoneCode("12345")
            .paymentType(PaymentTypeEnum.ORDER)
            .paymentMethod(PaymentMethodEnum.KAKAO)
            .paymentInfo(
                KakaoPayMethod.builder()
                    .partnerUserId("4")
                    .partnerOrderId("e2305c0f-c6b7-416c-aa00-3b03e141484a")
                    .pgToken("2b7ec102c68aea5cb6dd")
                    .tid("T58bae873c8776d3c60a")
                    .build())
            .build();

    OrderInfoDto orderInfoDto =
        OrderInfoDto.builder()
            .userPointUpdateDto(pointUpdateDto)
            .userCouponUpdateDto(couponUpdateDto)
            .productUpdateDto(productUpdateDtos)
            .orderCreationDto(orderCreationDto)
            .build();

    Long recipientId = 1L;
    RecipientTypeEnum recipientType = RecipientTypeEnum.ROLE_CONSUMER;
    NotificationTypeEnum notificationType = NotificationTypeEnum.INTERNAL_CONSUMER_SERVER_ERROR;

    ServerErrorForNotificationDto serverErrorDto =
        ServerErrorForNotificationDto.builder()
            .recipientId(recipientId)
            .recipientType(recipientType)
            .notificationType(notificationType)
            .error(orderInfoDto)
            .build();

    notificationService.sendError(serverErrorDto);
    ValueOperations<String, String> stringStringValueOperations = redisTemplate.opsForValue();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    String redisKey = "CONSUMER_" + recipientId;
    String redisValue = stringStringValueOperations.get(redisKey);
    assertThat(redisValue).isNotEqualTo(null);
  }
}
