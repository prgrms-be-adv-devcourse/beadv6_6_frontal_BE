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
public class AutoPurchaseConfirmationJobConfig {

    public static final String JOB_NAME = "autoPurchaseConfirmationJob";
    public static final String STEP_NAME = "autoPurchaseConfirmationStep";

    private final OrderRepository orderRepository;
    private final OrderUseCase orderUseCase;

    @Bean
    public Job autoPurchaseConfirmationJob(JobRepository jobRepository, Step autoPurchaseConfirmationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(autoPurchaseConfirmationStep)
                .build();
    }

    @Bean
    public Step autoPurchaseConfirmationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime threshold = LocalDateTime.now().minusDays(7);
                    log.info("[Batch] Starting auto purchase confirmation job. Threshold time: {}", threshold);

                    List<Order> paidOrders = orderRepository.findByStatusAndUpdatedAtBefore(
                            OrderStatus.PAID,
                            threshold
                    );

                    log.info("[Batch] Found {} orders to auto-confirm purchase.", paidOrders.size());

                    for (Order order : paidOrders) {
                        try {
                            log.info("[Batch] Confirming purchase automatically. OrderId={}, UserId={}", order.getId(), order.getUserId());
                            orderUseCase.completeOrder(order.getUserId(), order.getId());
                        } catch (Exception e) {
                            log.error("[Batch] Failed to auto-confirm order. OrderId={}", order.getId(), e);
                        }
                    }

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
