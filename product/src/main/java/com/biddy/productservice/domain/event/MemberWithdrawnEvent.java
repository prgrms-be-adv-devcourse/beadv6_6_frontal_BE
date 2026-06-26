package com.biddy.productservice.domain.event;

public record MemberWithdrawnEvent(
        Long memberId
) {}