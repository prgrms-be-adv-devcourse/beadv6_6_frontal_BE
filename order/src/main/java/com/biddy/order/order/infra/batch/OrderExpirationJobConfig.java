package com.biddy.order.order.infra.batch;

import com.biddy.order.order.application.usecase.OrderUseCase;
import com.biddy.order.order.domain.model.Order;
import com.biddy.order.order.domain.model.OrderStatus;
import com.biddy.order.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderExpirationJobConfig {

    public static final String JOB_NAME = "orderExpirationJob";
    public static final String STEP_NAME = "orderExpirationStep";

    private final OrderRepository orderRepository;
    private final OrderUseCase orderUseCase;

    @Bean
    public Job orderExpirationJob(JobRepository jobRepository, Step orderExpirationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(orderExpirationStep)
                .build();
    }

    @Bean
    public Step orderExpirationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
                    LocalDateTime now = LocalDateTime.now();
                    log.info("[Batch] Starting order expiration job. Normal threshold: {}, Current time: {}", threshold, now);

                    List<Order> expiredOrders = orderRepository.findExpiredOrders(
                            List.of(OrderStatus.PENDING, OrderStatus.PROCESSING),
                            threshold,
                            now
                    );

                    log.info("[Batch] Found {} expired orders to cancel.", expiredOrders.size());

                    for (Order order : expiredOrders) {
                        try {
                            log.info("[Batch] Cancelling expired order. OrderId={}, UserId={}", order.getId(), order.getUserId());
                            orderUseCase.cancelOrder(order.getUserId(), order.getId());
                        } catch (Exception e) {
                            log.error("[Batch] Failed to cancel expired order. OrderId={}", order.getId(), e);
                        }
                    }

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
