-- 집합체별 PENDING head 조회와 SKIP LOCKED 선점을 위한 복합 인덱스.

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_status_id
    ON outbox_events (aggregate_id, status, id);

-- 롤백 시에도 성능 인덱스는 데이터에 영향을 주지 않으므로 유지한다.
