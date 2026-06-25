package com.biddy.productservice.application.service;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.model.ProductLike;
import com.biddy.productservice.domain.repository.ProductRepository;
import com.biddy.productservice.infra.persistence.ProductLikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductLikeService {

    private final ProductLikeJpaRepository productLikeJpaRepository;
    private final ProductRepository productRepository;

    // 찜하기
    public void like(Long productId, Long memberId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));

        if (productLikeJpaRepository.findByProductIdAndMemberId(productId, memberId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 찜한 상품입니다.");
        }

        productLikeJpaRepository.save(new ProductLike(productId, memberId));
    }

    // 찜 취소
    public void unlike(Long productId, Long memberId) {
        ProductLike like = productLikeJpaRepository.findByProductIdAndMemberId(productId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "찜하지 않은 상품입니다."));
        productLikeJpaRepository.delete(like);
    }

    // 찜 개수 조회
    @Transactional(readOnly = true)
    public int getLikeCount(Long productId) {
        return productLikeJpaRepository.countByProductId(productId);
    }

    // 찜 여부 조회
    @Transactional(readOnly = true)
    public boolean isLiked(Long productId, Long memberId) {
        return productLikeJpaRepository.findByProductIdAndMemberId(productId, memberId).isPresent();
    }

    // 내 찜 목록
    @Transactional(readOnly = true)
    public List<Product> getLikedProducts(Long memberId) {
        List<Long> productIds = productLikeJpaRepository.findByMemberId(memberId)
                .stream()
                .map(ProductLike::getProductId)
                .toList();
        return productRepository.findAllById(productIds);
    }
}
