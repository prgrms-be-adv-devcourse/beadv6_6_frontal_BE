package com.biddy.productservice.infra.persistence;

import com.biddy.productservice.domain.model.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLike, Long> {

    Optional<ProductLike> findByProductIdAndMemberId(Long productId, Long memberId);

    int countByProductId(Long productId);

    List<ProductLike> findByMemberId(Long memberId);

    void deleteByProductIdAndMemberId(Long productId, Long memberId);
}
