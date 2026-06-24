package com.biddy.order.cart.infra.persistence;

import com.biddy.order.cart.domain.model.Cart;
import com.biddy.order.cart.domain.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository cartJpaRepository;

    @Override
    public List<Cart> findByUserId(Long userId) {
        return cartJpaRepository.findByUserId(userId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        cartJpaRepository.deleteByUserId(userId); // JPA 호출
    }

    @Override
    public Optional<Cart> findByUserIdAndProductId(Long userId, UUID productId) {
        return cartJpaRepository.findByUserIdAndProductId(userId,productId);
    }

    @Override
    public void deleteByUserIdAndProductId(Long userId, UUID productId) {
        cartJpaRepository.deleteByUserIdAndProductId(userId,productId); // JPA 호출
    }


    @Override
    public Cart save(Cart cart) {
        return cartJpaRepository.save(cart);
    }

    @Override
    public void delete(Cart cart) {
        cartJpaRepository.delete(cart);
    }
    @Override
    public Optional<Cart> findById(Long id) {
        return cartJpaRepository.findById(id); // JPA 내장 findById 호출
    }
}
