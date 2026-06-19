package com.biddy.order.cart.application.service;

import com.biddy.order.cart.application.dto.CartResult;
import com.biddy.order.cart.application.usecase.CartUseCase;
import com.biddy.order.cart.domain.model.Cart;
import com.biddy.order.cart.domain.repository.CartRepository;
import com.biddy.order.cart.infra.client.ProductApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartApplicationService implements CartUseCase {

    private final CartRepository cartRepository;
    private final ProductApiClient productApiClient;

    @Override
    @Transactional
    public CartResult addItemToCart(Long userId, UUID productId) {
        //1. 중복확인
        cartRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(existingCart -> {
                    throw new IllegalStateException("이미 장바구니에 존재하는 상품입니다.");
                });
        //2. 중복없으면 저장
        Cart cart = cartRepository.save(new Cart(userId, productId));

        return toResponse(cart);
    }

    @Override
    public List<CartResult> getCartList(Long userId) {
        // 1. 해당 유저의 장바구니 리스트 DB 조회
        List<Cart> carts = cartRepository.findByUserId(userId);
        if (carts.isEmpty()) {
            return List.of();
        }
        // 2. 장바구니 내 상품들의 UUID 리스트 추출
        List<UUID> productIds = carts.stream()
                .map(Cart::getProductId)
                .toList();
        // 3. RestTemplate을 가지고 상품 서버(8082포트) API 직접 호출
        List<ProductApiClient.ProductResponse> products = productApiClient.getProductsBulk(productIds);
        // 4. 상품 ID를 Key로 하는 Map으로 가공 (빠른 매칭을 위해)
        Map<UUID, ProductApiClient.ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(ProductApiClient.ProductResponse::productId, p -> p));
        // 5. 장바구니 리스트와 상품 정보를 결합하여 반환
        return carts.stream()
                .map(cart -> {
                    ProductApiClient.ProductResponse product = productMap.get(cart.getProductId());
                    return new CartResult(
                            cart.getId(),
                            cart.getUserId(),
                            cart.getProductId(),
                            product != null ? product.name() : "알 수 없는 상품",
                            product != null ? product.price() : BigDecimal.valueOf(0.0),
                            product != null ? product.status() : "UNKNOWN",
                            product != null ? product.userId() : null, // 판매자 ID
                            cart.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public void deleteCartItem(Long userId, Long cartId) {
        // 1. 삭제할 장바구니 아이템이 존재하는지 조회
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장바구니 아이템입니다."));
        // 2. 권한 검증: 로그인한 유저 ID와 장바구니 아이템의 유저 ID가 일치하는지 확인
        if (!cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 장바구니 아이템에 대한 삭제 권한이 없습니다.");
        }
        // 3. 삭제 처리
        cartRepository.delete(cart);
    }

    @Override
    @Transactional
    public void cleanCart(Long userId) {
        // 해당 사용자의 장바구니 전체 삭제
        cartRepository.deleteByUserId(userId);
    }

    private CartResult toResponse(Cart cart){
        return new CartResult(
                cart.getId(),
                cart.getUserId(),
                cart.getProductId(),
                null,
                null,
                null,
                null,
                cart.getCreatedAt()
        );
    }
}
