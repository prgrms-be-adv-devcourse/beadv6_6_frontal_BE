package com.biddy.order.order.infra.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job orderExpirationJob;
    private final Job autoPurchaseConfirmationJob;

    // YAML에 지정된 "0 */5 * * * *" 크론 주기에 따라 실행 (5분마다)
    @Scheduled(cron = "${order.batch.expiration-cron}")
    public void runOrderExpirationJob() {
        try {
            log.info("[Scheduler] Starting automatic order expiration job.");
            JobExecution jobExecution = jobLauncher.run(orderExpirationJob, new JobParametersBuilder()
                    .addString("requestedAt", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[Scheduler] Order expiration job execution completed. Status: {}", jobExecution.getStatus());
        } catch (Exception e) {
            log.error("[Scheduler] Order expiration job failed.", e);
        }
    }

    // YAML에 지정된 "0 0 2 * * *" 크론 주기에 따라 실행 (매일 새벽 2시)
    @Scheduled(cron = "${order.batch.purchase-confirmation-cron}")
    public void runAutoPurchaseConfirmationJob() {
        try {
            log.info("[Scheduler] Starting automatic purchase confirmation job.");
            JobExecution jobExecution = jobLauncher.run(autoPurchaseConfirmationJob, new JobParametersBuilder()
                    .addString("requestedAt", LocalDateTime.now().toString())
                    .toJobParameters());
            log.info("[Scheduler] Auto purchase confirmation job execution completed. Status: {}", jobExecution.getStatus());
        } catch (Exception e) {
            log.error("[Scheduler] Auto purchase confirmation job failed.", e);
        }
    }
}
