package com.biddy.order.order.presentation.controller;

import com.biddy.order.order.presentation.dto.response.BatchJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("${api.init}/orders/batch")
@RequiredArgsConstructor
@Tag(name = "Order Batch", description = "주문 배치잡 API")
public class OrderBatchController {

    private final JobLauncher jobLauncher;
    private final Job orderExpirationJob;
    private final Job autoPurchaseConfirmationJob;

    @PostMapping("/expiration")
    @Operation(summary = "결제 대기 만료 주문 취소 배치 실행", description = "30분이 경과한 PENDING/PROCESSING 주문을 취소 처리하는 Batch Job을 실행합니다.")
    public ResponseEntity<BatchJobResponse> runExpirationJob() throws Exception {
        JobExecution jobExecution = jobLauncher.run(orderExpirationJob, new JobParametersBuilder()
                .addString("requestedAt", LocalDateTime.now().toString())
                .toJobParameters());
        return ResponseEntity.ok(BatchJobResponse.from(jobExecution));
    }

    @PostMapping("/purchase-confirmation")
    @Operation(summary = "자동 구매 확정 배치 실행", description = "결제 완료 후 7일이 경과한 주문을 자동으로 구매 확정 처리하는 Batch Job을 실행합니다.")
    public ResponseEntity<BatchJobResponse> runPurchaseConfirmationJob() throws Exception {
        JobExecution jobExecution = jobLauncher.run(autoPurchaseConfirmationJob, new JobParametersBuilder()
                .addString("requestedAt", LocalDateTime.now().toString())
                .toJobParameters());
        return ResponseEntity.ok(BatchJobResponse.from(jobExecution));
    }
}
