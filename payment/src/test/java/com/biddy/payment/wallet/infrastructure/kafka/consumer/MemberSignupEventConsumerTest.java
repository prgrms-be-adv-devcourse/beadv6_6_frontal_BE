package com.biddy.payment.wallet.infrastructure.kafka.consumer;

import static org.mockito.Mockito.verify;

import com.biddy.payment.wallet.application.DepositService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MemberSignupEventConsumerTest {

    private final DepositService depositService = Mockito.mock(DepositService.class);
    private final MemberSignupEventConsumer consumer = new MemberSignupEventConsumer(
            depositService,
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void consume_opensDepositAccountForMember() {
        Long memberId = 1L;
        String payload = """
                {
                  "eventId": "%s",
                  "memberId": %d,
                  "signedUpAt": "%s"
                }
                """.formatted(UUID.randomUUID(), memberId, LocalDateTime.now());

        consumer.consume(payload);

        verify(depositService).openAccount(memberId);
    }
}
