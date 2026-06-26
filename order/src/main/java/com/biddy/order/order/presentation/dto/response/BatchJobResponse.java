package com.biddy.order.order.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.batch.core.JobExecution;

import java.time.LocalDateTime;

@Schema(description = "배치잡 실행 결과 응답")
public record BatchJobResponse(
        @Schema(description = "Job Execution ID") Long jobExecutionId,
        @Schema(description = "Job Instance ID") Long jobInstanceId,
        @Schema(description = "Job 이름") String jobName,
        @Schema(description = "실행 상태") String status,
        @Schema(description = "시작 시각") LocalDateTime startTime,
        @Schema(description = "종료 시각") LocalDateTime endTime
) {
    public static BatchJobResponse from(JobExecution jobExecution) {
        return new BatchJobResponse(
                jobExecution.getId(),
                jobExecution.getJobInstance().getId(),
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus().name(),
                jobExecution.getStartTime(),
                jobExecution.getEndTime()
        );
    }
}
