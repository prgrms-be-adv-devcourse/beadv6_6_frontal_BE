package com.biddy.memberservice.infrastructure.kafka;

import com.biddy.memberservice.application.event.MemberEventPublisher;
import com.biddy.memberservice.application.event.MemberSignupEvent;
import com.biddy.memberservice.application.event.MemberWithdrawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMemberEventPublisher implements MemberEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void publishSignup(Long memberId) {
        MemberSignupEvent event = new MemberSignupEvent(memberId);
        kafkaTemplate.send("member-signup", objectMapper.writeValueAsString(event));
    }

    @Override
    @SneakyThrows
    public void publishWithdraw(Long memberId) {
        MemberWithdrawEvent event = new MemberWithdrawEvent(memberId);
        kafkaTemplate.send("member-withdraw", objectMapper.writeValueAsString(event));
    }
}
