package com.jeontongju.notification.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.jeontongju.notification.domain.Notification;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class EmitterRepositoryImpl implements EmitterRepository {

  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final Map<String, Notification> eventCache = new ConcurrentHashMap<>();

  @Override
  public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
    emitters.put(emitterId, sseEmitter);
    return sseEmitter;
  }

  @Override
  public void deletedById(String emitterId) {
    emitters.remove(emitterId);
  }

  @Override
  public Map<String, Object> findAllEventCacheStartWithByEmail(String email) {

    return eventCache.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(email))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public Map<String, SseEmitter> findAllEmitterStartWithByEmail(String email) {
    return emitters.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(email))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public void saveEventCache(String key, Notification event) {
    eventCache.put(key, event);
  }
}
