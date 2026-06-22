package com.biddy.payment.wallet.infrastructure.kafka.consumer;

import com.biddy.payment.wallet.application.DepositService;
import com.biddy.payment.wallet.domain.event.MemberSignupEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MemberSignupEventConsumer {

    private final DepositService depositService;
    private final ObjectMapper objectMapper;

    public MemberSignupEventConsumer(DepositService depositService, ObjectMapper objectMapper) {
        this.depositService = depositService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "member-signup", groupId = "payment-service-wallet")
    public void consume(String payload) {
        try {
            MemberSignupEvent event = objectMapper.readValue(payload, MemberSignupEvent.class);
            depositService.openAccount(event.memberId());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("MemberSignupEvent payload를 읽을 수 없습니다.", exception);
        }
    }
}
