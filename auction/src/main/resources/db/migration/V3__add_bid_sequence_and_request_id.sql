-- 실시간 입찰 순서와 요청 멱등성을 위한 PostgreSQL 스키마 변경.
-- 운영 데이터가 있는 환경을 고려해 nullable 컬럼 추가 -> 백필 -> NOT NULL 순으로 적용한다.

ALTER TABLE auction
    ADD COLUMN IF NOT EXISTS bid_sequence BIGINT;

ALTER TABLE bid
    ADD COLUMN IF NOT EXISTS sequence BIGINT,
    ADD COLUMN IF NOT EXISTS request_id UUID;

-- 기존 입찰은 bid_at, bid_id 순으로 경매별 sequence를 결정한다.
-- bid_id를 보조 정렬로 사용해 같은 시각의 입찰도 항상 같은 순서로 백필한다.
WITH ranked_bid AS (
    SELECT bid_id,
           ROW_NUMBER() OVER (
               PARTITION BY auction_id
               ORDER BY bid_at ASC, bid_id ASC
           ) AS generated_sequence
    FROM bid
)
UPDATE bid AS target
SET sequence = ranked_bid.generated_sequence
FROM ranked_bid
WHERE target.bid_id = ranked_bid.bid_id
  AND target.sequence IS NULL;

-- 기존 요청에는 재현 가능한 UUID를 부여한다. bid_id가 전역 고유하므로 충돌하지 않는다.
UPDATE bid
SET request_id = MD5('legacy-bid:' || bid_id::TEXT)::UUID
WHERE request_id IS NULL;

-- Auction의 현재 sequence는 백필된 마지막 Bid에 맞춘다.
UPDATE auction AS target
SET bid_sequence = COALESCE(source.max_sequence, 0)
FROM (
    SELECT auction_id, MAX(sequence) AS max_sequence
    FROM bid
    GROUP BY auction_id
) AS source
WHERE target.auction_id = source.auction_id
  AND (target.bid_sequence IS NULL OR target.bid_sequence < source.max_sequence);

UPDATE auction
SET bid_sequence = 0
WHERE bid_sequence IS NULL;

ALTER TABLE auction
    ALTER COLUMN bid_sequence SET DEFAULT 0,
    ALTER COLUMN bid_sequence SET NOT NULL;

ALTER TABLE bid
    ALTER COLUMN sequence SET NOT NULL,
    ALTER COLUMN request_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_bid_auction_sequence
    ON bid (auction_id, sequence);

CREATE UNIQUE INDEX IF NOT EXISTS uk_bid_bidder_request
    ON bid (bidder_id, request_id);

COMMENT ON COLUMN auction.bid_sequence IS '경매별 성공 입찰의 마지막 sequence';
COMMENT ON COLUMN bid.sequence IS '경매별 성공 입찰 sequence';
COMMENT ON COLUMN bid.request_id IS '입찰자 범위 요청 멱등성 UUID';

-- 이전 애플리케이션은 추가 컬럼을 읽지 않으므로 긴급 롤백 시에도 컬럼과 인덱스를 유지한다.
