package com.biddy.recommendation.infra.persistence;

import com.biddy.recommendation.domain.model.ProductEmbedding;
import com.biddy.recommendation.domain.repository.ProductEmbeddingRepository;
import com.biddy.recommendation.util.VectorTextUtils;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// [2026-07-14] 예전에는 @RequiredArgsConstructor(Lombok)로 생성자를 자동 생성하고, 필드에
// @Qualifier("productJdbcTemplate")를 붙였었음. 이 어댑터가 엉뚱한 DB로 연결되는 버그가 있었는데
// 정확한 원인은 확정하지 못함 — "Lombok이 필드의 @Qualifier를 생성자로 안 옮겨줬다"는 추측을 했었지만,
// 같은 버그가 애초에 @Qualifier가 없는 다른 어댑터(UserInterestRepositoryAdapter)에서도 똑같이
// 발생해서 그 추측은 근거가 부족함(반증됨). 원인 불확실하지만, Lombok 생성자 대신 직접 쓴 생성자로
// 바꾸고 ProductDbConfig의 빈 이름도 Spring Boot 관례와 안 겹치게 바꾼 뒤, 진단 로그로 실제
// 연결 상태를 확인하는 중.
@Slf4j
@Repository
public class ProductEmbeddingRepositoryAdapter implements ProductEmbeddingRepository {

    // product-service가 소유한 DB(biddy_product)에 직접 연결되는 JdbcTemplate — 임베딩 테이블이 그쪽에 있음
    private final JdbcTemplate jdbcTemplate;

    public ProductEmbeddingRepositoryAdapter(@Qualifier("productJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void logDataSource() {
        if (jdbcTemplate.getDataSource() instanceof HikariDataSource hikari) {
            log.info(">>> [진단] ProductEmbeddingRepositoryAdapter가 실제로 물고 있는 jdbcUrl = {}", hikari.getJdbcUrl());
        }
    }

    // [2026-07-15] product_embedding 쓰기 경로는 product-service API(ProductClient.saveEmbedding)로 이전함.
    // recommendation은 이 테이블에 대해 조회만 하고, 쓰기는 더 이상 여기서 직접 하지 않음.
    // 예전 코드:
    // @Override
    // public ProductEmbedding save(ProductEmbedding embedding) {
    //     jdbcTemplate.update("""
    //             INSERT INTO product_embedding (product_id, embedding, source_text, category, updated_at)
    //             VALUES (?, CAST(? AS vector), ?, ?, now())
    //             ON CONFLICT (product_id)
    //             DO UPDATE SET embedding = EXCLUDED.embedding,
    //                           source_text = EXCLUDED.source_text,
    //                           category = EXCLUDED.category,
    //                           updated_at = EXCLUDED.updated_at
    //             """,
    //             embedding.getProductId(),
    //             VectorTextUtils.toText(embedding.getEmbedding()),
    //             embedding.getSourceText(),
    //             embedding.getCategory());
    //     return embedding;
    // }

    @Override
    public Optional<ProductEmbedding> findById(Long productId) {
        List<ProductEmbedding> results = jdbcTemplate.query("""
                SELECT product_id, embedding::text AS embedding, source_text, category, updated_at
                FROM product_embedding WHERE product_id = ?
                """,
                (rs, rowNum) -> ProductEmbedding.reconstruct(
                        rs.getLong("product_id"),
                        VectorTextUtils.fromText(rs.getString("embedding")),
                        rs.getString("source_text"),
                        rs.getString("category"),
                        rs.getTimestamp("updated_at").toLocalDateTime()),
                productId);
        return results.stream().findFirst();
    }

    @Override
    public List<Long> findNearestProductIds(float[] queryVector, List<Long> excludeProductIds, List<String> categories, int limit) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (!excludeProductIds.isEmpty()) {
            String placeholders = excludeProductIds.stream().map(id -> "?").collect(Collectors.joining(","));
            conditions.add("product_id NOT IN (" + placeholders + ")");
            params.addAll(excludeProductIds);
        }
        if (categories != null && !categories.isEmpty()) {
            String placeholders = categories.stream().map(c -> "?").collect(Collectors.joining(","));
            conditions.add("category IN (" + placeholders + ")");
            params.addAll(categories);
        }

        String whereClause = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);

        params.add(VectorTextUtils.toText(queryVector));
        params.add(limit);

        String sql = """
                SELECT product_id FROM product_embedding
                %s
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """.formatted(whereClause);

        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("product_id"), params.toArray());
    }

    @Override
    public List<Long> findProductIdsByCategory(String category, int limit) {
        return jdbcTemplate.query("""
                SELECT product_id FROM product_embedding
                WHERE category = ?
                ORDER BY updated_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> rs.getLong("product_id"),
                category, limit);
    }
}
