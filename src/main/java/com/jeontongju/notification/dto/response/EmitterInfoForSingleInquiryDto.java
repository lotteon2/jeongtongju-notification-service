package com.jeontongju.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class EmitterInfoForSingleInquiryDto {

    private String emitterId;
    private SseEmitter sseEmitter;
}
