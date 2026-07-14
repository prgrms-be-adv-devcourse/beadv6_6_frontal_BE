package com.biddy.productservice.infra.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// product_embedding 테이블은 product-service(biddy_product DB)가 소유함.
// recommendation-service는 이 테이블에 쓰기 권한이 없고, 임베딩 계산 후 이 API를 호출해서 저장을 위임함.
// (recommendation은 조회만 자기 datasource로 직접 함 - ProductDbConfig 참고)
@Repository
@RequiredArgsConstructor
public class ProductEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Long productId, float[] embedding, String sourceText, String category) {
        jdbcTemplate.update("""
                INSERT INTO product_embedding (product_id, embedding, source_text, category, updated_at)
                VALUES (?, CAST(? AS vector), ?, ?, now())
                ON CONFLICT (product_id)
                DO UPDATE SET embedding = EXCLUDED.embedding,
                              source_text = EXCLUDED.source_text,
                              category = EXCLUDED.category,
                              updated_at = EXCLUDED.updated_at
                """,
                productId, toVectorText(embedding), sourceText, category);
    }

    private String toVectorText(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }
}
