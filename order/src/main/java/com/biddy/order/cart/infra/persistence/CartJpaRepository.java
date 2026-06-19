package com.biddy.order.cart.infra.persistence;

import com.biddy.order.cart.domain.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndProductId(Long userId, UUID productId);

    Optional<Cart> findByUserIdAndProductId(Long userId, UUID productId);

}
