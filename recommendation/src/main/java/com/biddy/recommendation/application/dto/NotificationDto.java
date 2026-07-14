package com.biddy.recommendation.application.dto;

import java.time.LocalDateTime;

public record NotificationDto(Long productId, String message, LocalDateTime createdAt) {

    public static NotificationDto from(com.biddy.recommendation.domain.model.MatchNotification notification) {
        return new NotificationDto(notification.getProductId(), notification.getMessage(), notification.getCreatedAt());
    }
}
