package com.biddy.order.cart.application.usecase;

import com.biddy.order.cart.application.dto.CartResult;
import com.biddy.order.cart.domain.model.Cart;

import java.util.List;
public interface CartUseCase {
    CartResult addItemToCart(Long userId, Long productId);
    List<CartResult> getCartList(Long userId);
    void deleteCartItem(Long userId, Long cartId);
    void cleanCart(Long userId);
}
