package com.biddy.recommendation.domain.repository;

import com.biddy.recommendation.domain.model.MatchNotification;

import java.util.List;

public interface MatchNotificationRepository {

    MatchNotification save(MatchNotification notification);

    List<MatchNotification> findByMemberId(Long memberId);

    List<MatchNotification> findUnreadByMemberId(Long memberId);

    void markAllAsRead(Long memberId);
}
