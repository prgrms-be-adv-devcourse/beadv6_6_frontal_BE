package com.biddy.payment.wallet.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberSignupEvent(
        UUID eventId,
        Long memberId,
        LocalDateTime signedUpAt
) {
}
