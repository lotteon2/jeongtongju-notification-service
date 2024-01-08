package com.jeontongju.notification.repository;

import java.util.Map;

import com.jeontongju.notification.domain.Notification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface EmitterRepository {

  SseEmitter save(String emitterId, SseEmitter sseEmitter); // emitter 저장

  void deletedById(String emitterId);

  Map<String, Object> findAllEventCacheStartWithByEmail(String email);

  Map<String, SseEmitter> findAllEmitterStartWithByEmail(String email);

  void saveEventCache(String key, Notification event);
}
