-- Auction AWS k6 전용 경매 준비
--
-- 관점: 성능 테스트가 운영 경매와 기존 Bid 이력에 영향을 주지 않도록 데이터를 격리한다.
-- 개선 목적: 재실행 가능한 동일 초기 상태를 만들어 변경 전후 결과를 비교한다.
--
-- 실행 예시:
-- psql -h <HOST> -p 15432 -U <USER> -d biddy_auction \
--   -v auction_id=A-K6-HOT01 \
--   -v product_id=900001 \
--   -v seller_id=900001 \
--   -v start_price=100000 \
--   -v min_increment=1000 \
--   -v ends_in_seconds=3600 \
--   -f k6-tests/scripts/setup_auction_aws_test_data.sql

\set ON_ERROR_STOP on

\if :{?auction_id}
\else
  \echo 'auction_id is required'
  \quit
\endif
\if :{?product_id}
\else
  \echo 'product_id is required'
  \quit
\endif
\if :{?seller_id}
\else
  \echo 'seller_id is required'
  \quit
\endif
\if :{?start_price}
\else
  \set start_price 100000
\endif
\if :{?min_increment}
\else
  \set min_increment 1000
\endif
\if :{?ends_in_seconds}
\else
  \set ends_in_seconds 3600
\endif

SELECT :'auction_id' LIKE 'A-K6-%' AS safe_auction_id \gset
\if :safe_auction_id
\else
  \echo 'REFUSED: auction_id must start with A-K6-'
  \quit
\endif

BEGIN;

-- 같은 테스트 ID의 이전 실행 데이터만 초기화한다.
DELETE FROM public.auction_watch WHERE auction_id = :'auction_id';
DELETE FROM public.bid WHERE auction_id = :'auction_id';
DELETE FROM public.auction WHERE auction_id = :'auction_id';

INSERT INTO public.auction (
    auction_id,
    product_id,
    seller_id,
    start_price,
    min_increment,
    current_bid,
    current_bidder_id,
    bid_count,
    watcher_count,
    status,
    starts_at,
    ends_at,
    winner_id,
    winning_bid_id,
    created_at,
    updated_at
) VALUES (
    :'auction_id',
    (:'product_id')::BIGINT,
    (:'seller_id')::BIGINT,
    (:'start_price')::BIGINT,
    (:'min_increment')::BIGINT,
    (:'start_price')::BIGINT,
    NULL,
    0,
    0,
    'LIVE',
    CURRENT_TIMESTAMP - INTERVAL '1 minute',
    CURRENT_TIMESTAMP + ((:'ends_in_seconds')::INTEGER * INTERVAL '1 second'),
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

COMMIT;

SELECT
    auction_id,
    product_id,
    seller_id,
    current_bid,
    current_bidder_id,
    bid_count,
    status,
    starts_at,
    ends_at
FROM public.auction
WHERE auction_id = :'auction_id';

\echo 'Auction k6 test data is ready.'
