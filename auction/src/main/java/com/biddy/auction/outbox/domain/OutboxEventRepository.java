package com.biddy.auction.outbox.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Outbox 이벤트 리포지토리.
 *
 * <p>Pessimistic locking을 사용하여 동시성 문제를 방지한다.</p>
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** 테스트·감사 조회용 집합체별 이벤트 순서 조회 */
    List<OutboxEvent> findByAggregateIdOrderByIdAsc(String aggregateId);

    /**
     * 집합체별 가장 오래된 PENDING 이벤트만 ID 순으로 선점한다.
     * 다른 Pod가 잠근 행은 기다리지 않고 건너뛰되, 같은 경매의 다음 이벤트는 앞선 이벤트가
     * PROCESSED/FAILED가 될 때까지 후보가 되지 않으므로 sequence 역전이 발생하지 않는다.
     */
    @Query(value = """
            SELECT candidate.*
            FROM outbox_events candidate
            WHERE candidate.status = 'PENDING'
              AND NOT EXISTS (
                  SELECT 1
                  FROM outbox_events earlier
                  WHERE earlier.aggregate_id = candidate.aggregate_id
                    AND earlier.status = 'PENDING'
                    AND earlier.id < candidate.id
              )
            ORDER BY candidate.id ASC
            LIMIT :batchSize
            FOR UPDATE OF candidate SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimPendingAggregateHeads(@Param("batchSize") int batchSize);
}
