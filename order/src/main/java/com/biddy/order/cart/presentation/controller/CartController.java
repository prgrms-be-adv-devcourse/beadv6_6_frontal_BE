package com.biddy.order.cart.presentation.controller;

import com.biddy.order.cart.application.dto.CartResult;
import com.biddy.order.cart.application.usecase.CartUseCase;
import com.biddy.order.cart.presentation.dto.request.AddCartItemRequest;
import com.biddy.order.cart.presentation.dto.response.CartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartUseCase cartUseCase;

    // 1. 장바구니 담기 (POST /cart/item)
    @PostMapping("/item")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody AddCartItemRequest request
    ) {
        CartResult cart = cartUseCase.addItemToCart(userId, request.productId());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    // 2. 장바구니 목록 조회 (GET /cart/list)
    @GetMapping("/list")
    public ResponseEntity<List<CartResponse>> getCartList(
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<CartResponse> response = cartUseCase.getCartList(userId).stream()
                .map(CartResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 3. 장바구니 상품 삭제 (DELETE /cart/delete)
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteItem(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("cartId") Long cartId
    ) {
        cartUseCase.deleteCartItem(userId, cartId);
        return ResponseEntity.ok("장바구니 상품이 삭제되었습니다.");
    }

    // 4. 전체 비우기 (DELETE /cart/clean)
    @DeleteMapping("/clean")
    public ResponseEntity<String> cleanCart(
            @RequestHeader("X-User-Id") Long userId
    ) {
        cartUseCase.cleanCart(userId);
        return ResponseEntity.ok("장바구니가 비워졌습니다.");
    }


}
