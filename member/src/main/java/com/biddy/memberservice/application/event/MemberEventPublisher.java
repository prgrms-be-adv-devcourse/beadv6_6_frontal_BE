package com.biddy.memberservice.application.event;




public interface MemberEventPublisher {
    void publishSignup(Long memberId);
    void publishWithdraw(Long memberId);
}
