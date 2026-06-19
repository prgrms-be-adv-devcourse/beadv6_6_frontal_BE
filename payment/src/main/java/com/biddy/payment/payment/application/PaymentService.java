package com.biddy.payment.payment.application;

import com.biddy.payment.payment.domain.event.PaymentCompletedEvent;
import com.biddy.payment.payment.domain.event.PaymentFailedEvent;
import com.biddy.payment.payment.domain.event.PaymentRefundedEvent;
import com.biddy.payment.wallet.domain.DepositTransactionType;
import com.biddy.payment.wallet.application.DepositService;
import com.biddy.payment.payment.domain.CancelType;
import com.biddy.payment.payment.domain.Payment;
import com.biddy.payment.payment.domain.PaymentCancel;
import com.biddy.payment.payment.domain.PaymentMethod;
import com.biddy.payment.payment.domain.PaymentStatus;
import com.biddy.payment.payment.infrastructure.client.order.OrderClient;
import com.biddy.payment.payment.infrastructure.client.order.OrderPaymentInfo;
import com.biddy.payment.payment.infrastructure.kafka.producer.PaymentEventProducer;
import com.biddy.payment.payment.presentation.request.PaymentCreateRequest;
import com.biddy.payment.payment.presentation.request.PaymentCancelRequest;
import com.biddy.payment.payment.presentation.response.PaymentCancelResponse;
import com.biddy.payment.payment.presentation.response.PaymentResponse;
import com.biddy.payment.payment.infrastructure.persistence.PaymentCancelRepository;
import com.biddy.payment.payment.infrastructure.persistence.PaymentRepository;
import com.biddy.payment.wallet.infrastructure.client.toss.TossPaymentClient;
import com.biddy.payment.wallet.infrastructure.client.toss.TossPaymentConfirmResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;
    private final DepositService depositService;
    private final OrderClient orderClient;
    private final PaymentEventProducer paymentEventProducer;
    private final TossPaymentClient tossPaymentClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentCancelRepository paymentCancelRepository,
            DepositService depositService,
            OrderClient orderClient,
            PaymentEventProducer paymentEventProducer,
            TossPaymentClient tossPaymentClient
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentCancelRepository = paymentCancelRepository;
        this.depositService = depositService;
        this.orderClient = orderClient;
        this.paymentEventProducer = paymentEventProducer;
        this.tossPaymentClient = tossPaymentClient;
    }

    @Transactional
    public PaymentResponse create(PaymentCreateRequest request) {
        if (paymentRepository.existsByOrderIdAndStatus(request.orderId(), PaymentStatus.COMPLETED)) {
            throw new IllegalStateException("이미 결제가 완료된 주문입니다.");
        }

        OrderPaymentInfo order = orderClient.getPaymentInfo(request.orderId());
        validateOrderPayment(request, order);
        orderClient.requestPaymentProcessing(order.orderId(), order.buyerId());

        Payment payment = paymentRepository.save(Payment.create(
                order.orderId(),
                order.buyerId(),
                order.sellerId(),
                order.amount(),
                request.paymentMethod(),
                request.pgTransactionId()
        ));
        payment.startProcessing();

        try {
            if (request.paymentMethod() == PaymentMethod.WALLET) {
                depositService.decreaseForPayment(order.buyerId(), order.amount(), String.valueOf(payment.getId()));
            }

            String pgTransactionId = resolvePgTransactionId(request, order);
            payment.complete(pgTransactionId);
            paymentEventProducer.publish(new PaymentCompletedEvent(
                    UUID.randomUUID(),
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getSellerId(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    LocalDateTime.now()
            ));
        } catch (RuntimeException exception) {
            payment.fail();
            paymentEventProducer.publish(new PaymentFailedEvent(
                    UUID.randomUUID(),
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getAmount(),
                    exception.getMessage(),
                    LocalDateTime.now()
            ));
            throw exception;
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        return toResponse(findPayment(id));
    }

    @Transactional
    public PaymentResponse cancel(Long id, PaymentCancelRequest request) {
        return processCancel(id, request, CancelType.CANCEL);
    }

    @Transactional
    public PaymentResponse refund(Long id, PaymentCancelRequest request) {
        return processCancel(id, request, CancelType.REFUND);
    }

    private PaymentResponse processCancel(Long id, PaymentCancelRequest request, CancelType cancelType) {
        Payment payment = findPayment(id);
        if (!payment.isCompleted()) {
            throw new IllegalStateException("완료된 결제만 취소 또는 환불할 수 있습니다.");
        }

        long totalCancelAmount = paymentCancelRepository.sumCancelAmountByPaymentId(id);
        if (totalCancelAmount + request.amount() > payment.getAmount()) {
            throw new IllegalArgumentException("취소/환불 누적 금액은 결제 금액을 초과할 수 없습니다.");
        }

        PaymentCancel cancel = PaymentCancel.create(id, cancelType, request.reason(), request.amount());
        paymentCancelRepository.save(cancel);

        if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
            DepositTransactionType type = cancelType == CancelType.CANCEL
                    ? DepositTransactionType.CANCEL
                    : DepositTransactionType.REFUND;
            depositService.increaseForCancel(payment.getUserId(), request.amount(), String.valueOf(payment.getId()), type, request.reason());
        }

        if (totalCancelAmount + request.amount() == payment.getAmount()) {
            if (cancelType == CancelType.CANCEL) {
                payment.cancel();
            } else {
                payment.refund();
                paymentEventProducer.publish(new PaymentRefundedEvent(
                        UUID.randomUUID(),
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getUserId(),
                        request.amount(),
                        request.reason(),
                        LocalDateTime.now()
                ));
            }
        }

        return toResponse(payment);
    }

    private void validateOrderPayment(PaymentCreateRequest request, OrderPaymentInfo order) {
        if (order == null) {
            throw new IllegalStateException("주문 결제 정보를 조회할 수 없습니다.");
        }
        if (!order.orderId().equals(request.orderId())) {
            throw new IllegalStateException("요청 주문과 조회된 주문이 일치하지 않습니다.");
        }
        if (!order.buyerId().equals(request.userId())) {
            throw new IllegalStateException("주문 구매자와 결제 요청자가 일치하지 않습니다.");
        }
        if (!order.amount().equals(request.amount())) {
            throw new IllegalStateException("주문 금액과 결제 금액이 일치하지 않습니다.");
        }
        if (!order.isPaymentPending()) {
            throw new IllegalStateException("결제 대기 상태인 주문만 결제할 수 있습니다.");
        }
        if (order.isExpired(LocalDateTime.now())) {
            throw new IllegalStateException("결제 제한 시간이 만료되었습니다.");
        }
    }

    private String resolvePgTransactionId(PaymentCreateRequest request, OrderPaymentInfo order) {
        if (request.paymentMethod() == PaymentMethod.NORMAL) {
            validateNormalPaymentRequest(request);
            TossPaymentConfirmResponse confirmResponse = tossPaymentClient.confirm(
                    request.paymentKey(),
                    request.tossOrderId(),
                    order.amount()
            );
            validateTossConfirmResponse(request, order, confirmResponse);
            return confirmResponse.paymentKey();
        }

        return request.pgTransactionId() != null
                ? request.pgTransactionId()
                : "wallet-" + order.orderId();
    }

    private void validateNormalPaymentRequest(PaymentCreateRequest request) {
        if (isBlank(request.paymentKey())) {
            throw new IllegalArgumentException("일반 결제는 Toss paymentKey가 필요합니다.");
        }
        if (isBlank(request.tossOrderId())) {
            throw new IllegalArgumentException("일반 결제는 Toss orderId가 필요합니다.");
        }
    }

    private void validateTossConfirmResponse(
            PaymentCreateRequest request,
            OrderPaymentInfo order,
            TossPaymentConfirmResponse response
    ) {
        if (response == null || !response.isDone()) {
            throw new IllegalStateException("Toss Payments 결제 승인에 실패했습니다.");
        }
        if (!Objects.equals(response.paymentKey(), request.paymentKey())) {
            throw new IllegalStateException("Toss Payments paymentKey가 요청과 일치하지 않습니다.");
        }
        if (!Objects.equals(response.orderId(), request.tossOrderId())) {
            throw new IllegalStateException("Toss Payments orderId가 요청과 일치하지 않습니다.");
        }
        if (!Objects.equals(response.totalAmount(), order.amount())) {
            throw new IllegalStateException("Toss Payments 승인 금액이 주문 금액과 일치하지 않습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("결제를 찾을 수 없습니다. id=" + id));
    }

    private PaymentResponse toResponse(Payment payment) {
        List<PaymentCancelResponse> cancels = paymentCancelRepository.findByPaymentIdOrderByCreatedAtDesc(payment.getId()).stream()
                .map(PaymentCancelResponse::from)
                .toList();
        return PaymentResponse.from(payment, cancels);
    }
}
