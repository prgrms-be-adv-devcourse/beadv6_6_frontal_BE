CREATE EXTENSION IF NOT EXISTS vector;

-- product_embedding 테이블은 product-service(biddy_product DB)가 소유함.
-- 정의는 product/src/main/resources/schema.sql 참고, 이 서비스는 별도 datasource(ProductDbConfig)로 직접 조회함.

CREATE TABLE IF NOT EXISTS user_interest (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    keyword TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS match_notification (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE match_notification ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT false;
