package com.biddy.recommendation.infra.persistence;

import com.biddy.recommendation.domain.model.MatchNotification;
import com.biddy.recommendation.domain.repository.MatchNotificationRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

// [2026-07-14] UserInterestRepositoryAdapter와 같은 이유로 명시적 @Qualifier + 진단 로그 추가.
@Slf4j
@Repository
public class MatchNotificationRepositoryAdapter implements MatchNotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public MatchNotificationRepositoryAdapter(@Qualifier("recommendationJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void logDataSource() {
        if (jdbcTemplate.getDataSource() instanceof HikariDataSource hikari) {
            log.info(">>> [진단] MatchNotificationRepositoryAdapter가 실제로 물고 있는 jdbcUrl = {}", hikari.getJdbcUrl());
        }
    }

    @Override
    public MatchNotification save(MatchNotification notification) {
        jdbcTemplate.update("""
                INSERT INTO match_notification (member_id, product_id, message, created_at)
                VALUES (?, ?, ?, now())
                """,
                notification.getMemberId(),
                notification.getProductId(),
                notification.getMessage());
        return notification;
    }

    @Override
    public List<MatchNotification> findByMemberId(Long memberId) {
        return jdbcTemplate.query("""
                SELECT member_id, product_id, message, created_at
                FROM match_notification
                WHERE member_id = ?
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> MatchNotification.reconstruct(
                        rs.getLong("member_id"),
                        rs.getLong("product_id"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                memberId);
    }

    @Override
    public List<MatchNotification> findUnreadByMemberId(Long memberId) {
        return jdbcTemplate.query("""
                SELECT member_id, product_id, message, created_at
                FROM match_notification
                WHERE member_id = ? AND is_read = false
                ORDER BY created_at DESC
                """,
                (rs, rowNum) -> MatchNotification.reconstruct(
                        rs.getLong("member_id"),
                        rs.getLong("product_id"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                memberId);
    }

    @Override
    public void markAllAsRead(Long memberId) {
        jdbcTemplate.update("""
                UPDATE match_notification SET is_read = true
                WHERE member_id = ? AND is_read = false
                """,
                memberId);
    }
}
