package com.biddy.recommendation.infra.persistence;

import com.biddy.recommendation.domain.model.UserInterest;
import com.biddy.recommendation.domain.repository.UserInterestRepository;
import com.biddy.recommendation.util.VectorTextUtils;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

// [2026-07-14] 이 어댑터는 애초에 @Qualifier를 쓴 적이 없었는데도(그냥 기본 JdbcTemplate을 기대) 실제로는
// biddy_product로 잘못 연결되는 버그가 있었음. 원인 미확정 — ProductDbConfig의 빈 이름이 Spring Boot
// 자동 설정 이름(dataSource/jdbcTemplate)과 겹쳤던 게 의심되지만 확실히 검증된 건 아님. 이번엔 여기도
// "기본값에 맡기기"를 그만두고 명시적으로 @Qualifier("recommendationJdbcTemplate")를 지정하고,
// 진단 로그로 실제 연결을 눈으로 확인하는 중 — 아직 확인 전이라 "고쳤다"고 단정하지 않음.
@Slf4j
@Repository
public class UserInterestRepositoryAdapter implements UserInterestRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserInterestRepositoryAdapter(@Qualifier("recommendationJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void logDataSource() {
        if (jdbcTemplate.getDataSource() instanceof HikariDataSource hikari) {
            log.info(">>> [진단] UserInterestRepositoryAdapter가 실제로 물고 있는 jdbcUrl = {}", hikari.getJdbcUrl());
        }
    }

    // [2026-07-14] 기존에는 Statement.RETURN_GENERATED_KEYS + KeyHolder.getKey()로 생성된 id를 받아왔으나,
    // PostgreSQL JDBC 드라이버는 RETURN_GENERATED_KEYS 요청 시 RETURNING *처럼 전체 컬럼을 돌려줘서
    // getKey()(단일 키 전용)가 "여러 키가 반환됨" 예외를 던지는 문제(500 에러)가 있었음.
    // id 값은 이후 로직에서 실제로 쓰이지 않아서, 생성된 키를 받아오는 부분 자체를 제거함.
    // 예전 코드:
    // KeyHolder keyHolder = new GeneratedKeyHolder();
    // jdbcTemplate.update((PreparedStatementCreator) connection -> {
    //     PreparedStatement ps = connection.prepareStatement("""
    //             INSERT INTO user_interest (member_id, keyword, embedding, created_at)
    //             VALUES (?, ?, CAST(? AS vector), now())
    //             """, Statement.RETURN_GENERATED_KEYS);
    //     ps.setLong(1, userInterest.getMemberId());
    //     ps.setString(2, userInterest.getKeyword());
    //     ps.setString(3, vectorText);
    //     return ps;
    // }, keyHolder);
    // userInterest.assignId(keyHolder.getKey().longValue());
    @Override
    public UserInterest save(UserInterest userInterest) {
        String vectorText = VectorTextUtils.toText(userInterest.getEmbedding());
        jdbcTemplate.update("""
                INSERT INTO user_interest (member_id, keyword, embedding, created_at)
                VALUES (?, ?, CAST(? AS vector), now())
                """,
                userInterest.getMemberId(),
                userInterest.getKeyword(),
                vectorText);
        return userInterest;
    }

    @Override
    public List<UserInterest> findMatchesWithin(float[] queryVector, double maxDistance) {
        String vectorText = VectorTextUtils.toText(queryVector);
        return jdbcTemplate.query("""
                SELECT id, member_id, keyword, embedding::text AS embedding, created_at
                FROM user_interest
                WHERE embedding <=> CAST(? AS vector) < ?
                ORDER BY embedding <=> CAST(? AS vector)
                """,
                (rs, rowNum) -> UserInterest.reconstruct(
                        rs.getLong("id"),
                        rs.getLong("member_id"),
                        rs.getString("keyword"),
                        VectorTextUtils.fromText(rs.getString("embedding")),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                vectorText, maxDistance, vectorText);
    }
}
