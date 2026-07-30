-- Outbox 멱등성 식별자와 순서 조회 인덱스.
-- 기존 행을 백필한 뒤 NOT NULL을 적용하여 운영 데이터가 있는 환경에서도 안전하게 전환한다.

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS event_id UUID;

UPDATE outbox_events
SET event_id = MD5('legacy-outbox:' || id::TEXT)::UUID
WHERE event_id IS NULL;

ALTER TABLE outbox_events
    ALTER COLUMN event_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_outbox_event_id
    ON outbox_events (event_id);

CREATE INDEX IF NOT EXISTS idx_outbox_status_id
    ON outbox_events (status, id);

COMMENT ON COLUMN outbox_events.event_id IS '소비자 중복 제거용 전역 이벤트 UUID';

-- 이전 애플리케이션으로 롤백해도 추가 컬럼과 인덱스는 유지한다.
