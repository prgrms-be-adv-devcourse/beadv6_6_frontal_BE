-- Auction AWS k6 전용 데이터 정리
-- 결과와 정합성 SQL을 먼저 보존한 뒤 실행한다.
-- A-K6- 접두사가 아닌 Auction은 삭제하지 않는다.
--
-- 실행 예시:
-- psql -h <HOST> -p 15432 -U <USER> -d biddy_auction \
--   -v auction_id=A-K6-HOT01 \
--   -f k6-tests/scripts/cleanup_auction_aws_test_data.sql

\set ON_ERROR_STOP on

\if :{?auction_id}
\else
  \echo 'auction_id is required'
  \quit
\endif

SELECT :'auction_id' LIKE 'A-K6-%' AS safe_auction_id \gset
\if :safe_auction_id
\else
  \echo 'REFUSED: auction_id must start with A-K6-'
  \quit
\endif

BEGIN;

DELETE FROM public.auction_watch WHERE auction_id = :'auction_id';
DELETE FROM public.bid WHERE auction_id = :'auction_id';
DELETE FROM public.auction WHERE auction_id = :'auction_id';

COMMIT;

SELECT COUNT(*) AS remaining_auction_rows
FROM public.auction
WHERE auction_id = :'auction_id';

\echo 'Auction k6 test data cleanup completed.'
