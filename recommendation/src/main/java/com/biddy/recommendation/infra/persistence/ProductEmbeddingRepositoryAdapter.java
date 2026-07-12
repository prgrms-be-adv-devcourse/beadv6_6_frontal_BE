package com.biddy.recommendation.infra.persistence;

import com.biddy.recommendation.domain.model.ProductEmbedding;
import com.biddy.recommendation.domain.repository.ProductEmbeddingRepository;
import com.biddy.recommendation.util.VectorTextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductEmbeddingRepositoryAdapter implements ProductEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ProductEmbedding save(ProductEmbedding embedding) {
        jdbcTemplate.update("""
                INSERT INTO product_embedding (product_id, embedding, source_text, category, updated_at)
                VALUES (?, CAST(? AS vector), ?, ?, now())
                ON CONFLICT (product_id)
                DO UPDATE SET embedding = EXCLUDED.embedding,
                              source_text = EXCLUDED.source_text,
                              category = EXCLUDED.category,
                              updated_at = EXCLUDED.updated_at
                """,
                embedding.getProductId(),
                VectorTextUtils.toText(embedding.getEmbedding()),
                embedding.getSourceText(),
                embedding.getCategory());
        return embedding;
    }

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
}
