CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS product_embedding (
    product_id BIGINT PRIMARY KEY,
    embedding vector(1536) NOT NULL,
    source_text TEXT NOT NULL,
    category VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE product_embedding ADD COLUMN IF NOT EXISTS category VARCHAR(50);
