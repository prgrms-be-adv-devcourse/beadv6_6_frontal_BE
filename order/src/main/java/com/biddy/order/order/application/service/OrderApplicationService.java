package com.biddy.order.order.application.service;

import com.biddy.order.order.application.dto.OrderPaymentInfoResult;
import com.biddy.order.order.application.dto.OrderResult;
import com.biddy.order.order.application.usecase.OrderUseCase;
import com.biddy.order.order.domain.model.Order;
import com.biddy.order.order.domain.model.OrderInfo;
import com.biddy.order.order.domain.model.OrderStatus;
import com.biddy.order.order.domain.repository.OrderRepository;
import com.biddy.order.order.infra.event.OrderEventPublisher;
import com.biddy.order.order.presentation.dto.request.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderApplicationService implements OrderUseCase {

    private final OrderRepository orderRepository;
    
    // 카프카 비활성화 시에도 기동이 실패하지 않도록 Optional로 감싸서 주입받음
    private final Optional<OrderEventPublisher> orderEventPublisher;

    @Override
    @Transactional
    public OrderResult createOrder(Long userId, CreateOrderRequest request) {
        // 1. 주문 총 금액 계산 (상품별 주문 단가 * 수량의 합계)
        long totalPrice = request.items().stream()
                .mapToLong(item -> item.orderPrice() * item.quantity())
                .sum();

        // 2. 주문서 빌드 (상태: PENDING)
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalPrice(totalPrice)
                .build();

        // 3. 주문 상세 정보(OrderInfo) 생성 및 관계 매핑
        List<OrderInfo> orderInfos = request.items().stream()
                .map(item -> OrderInfo.builder()
                        .order(order)
                        .orderPrice(item.orderPrice())
                        .quantity(item.quantity())
                        .productId(item.productId())
                        .build())
                .collect(Collectors.toList());

        order.getOrderInfos().addAll(orderInfos);

        // 4. DB 저장 (CascadeType.ALL에 의해 OrderInfo도 함께 저장됨)
        Order savedOrder = orderRepository.save(order);

        // 5. 카프카 이벤트 발행 (결제 요청 및 재고 차감)
        orderEventPublisher.ifPresent(publisher -> {
            savedOrder.getOrderInfos().forEach(info -> 
                publisher.publishDecreaseStock(info.getProductId(), info.getQuantity())
            );
        });

        return toResponse(savedOrder);
    }

    @Override
    public List<OrderResult> getOrderList(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OrderResult getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 주문에 대한 조회 권한이 없습니다.");
        }

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResult changeStatus(Long userId, Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 주문에 대한 수정 권한이 없습니다.");
        }

        order.updateStatus(status);

        // 상태값 변화에 따른 카프카 비즈니스 이벤트 트리거
        if (status == OrderStatus.CANCELLED) {
            triggerCancelEvents(order);
        } else if (status == OrderStatus.COMPLETED) {
            triggerCompleteEvents(order);
        }

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResult cancelOrder(Long userId, Long orderId) {
        return changeStatus(userId, orderId, OrderStatus.CANCELLED);
    }

    @Override
    @Transactional
    public OrderResult completeOrder(Long userId, Long orderId) {
        return changeStatus(userId, orderId, OrderStatus.COMPLETED);
    }

    @Override
    @Transactional
    public OrderResult startPaymentProcessing(Long userId, Long orderId) {
        return changeStatus(userId, orderId, OrderStatus.PROCESSING);
    }

    @Override
    public OrderPaymentInfoResult getPaymentInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return new OrderPaymentInfoResult(
                order.getId(),
                order.getUserId(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getUpdatedAt()
        );
    }

    // 주문 취소 시 카프카 연계 이벤트 발행 (결제취소, 재고원복)
    private void triggerCancelEvents(Order order) {
        orderEventPublisher.ifPresent(publisher -> {
            // 1. 결제 서비스에 결제 취소 요청 발송
            publisher.publishCancelPayment(order.getId(), order.getUserId());
            
            // 2. 각 상품들에 대한 재고 원복 요청 발송
            order.getOrderInfos().forEach(info -> 
                publisher.publishRestoreStock(info.getProductId(), info.getQuantity())
            );
        });
    }

    // 주문 완료 시 카프카 연계 이벤트 발행 (정산요청)
    private void triggerCompleteEvents(Order order) {
        orderEventPublisher.ifPresent(publisher -> 
            publisher.publishRequestSettlement(order.getId(), order.getUserId())
        );
    }

    // DTO 변환 헬퍼 메서드
    private OrderResult toResponse(Order order) {
        List<OrderResult.OrderInfoResult> infoResults = order.getOrderInfos().stream()
                .map(info -> new OrderResult.OrderInfoResult(
                        info.getId(),
                        info.getOrderPrice(),
                        info.getQuantity(),
                        info.getProductId(),
                        info.getCreatedAt()
                ))
                .toList();

        return new OrderResult(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                infoResults,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
