package com.biddy.order.cart.domain.repository;

import com.biddy.order.cart.domain.model.Cart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface CartRepository  {
    Cart save(Cart cart);
    List<Cart> findByUserId(Long userId);
    void deleteByUserIdAndProductId(Long userId, UUID productId);
    void deleteByUserId(Long userId);

    Optional<Cart> findByUserIdAndProductId(Long userId, UUID productId);

    Optional<Cart> findById(Long id);
    void delete(Cart cart);
}
