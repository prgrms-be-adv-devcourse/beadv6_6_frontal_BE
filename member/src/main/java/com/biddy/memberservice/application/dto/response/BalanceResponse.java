package com.biddy.memberservice.application.dto.response;

import com.biddy.memberservice.domain.model.Balance;
import com.biddy.memberservice.domain.model.BalanceHistory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class BalanceResponse {

    private Long memberId;
    private BigDecimal amount;
    private LocalDateTime updatedAt;
    private List<TradeHistoryDto> tradeList;

    public static BalanceResponse of(Balance balance, List<BalanceHistory> histories) {
        return BalanceResponse.builder()
                .memberId(balance.getMember().getId())
                .amount(balance.getAmount())
                .updatedAt(balance.getUpdatedAt())
                .tradeList(histories.stream()
                        .map(TradeHistoryDto::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class TradeHistoryDto {
        private Long historyId;
        private String type;
        private BigDecimal changeAmount;
        private BigDecimal balanceSnapshot;
        private String description;
        private LocalDateTime datetime;

        public static TradeHistoryDto from(BalanceHistory history) {
            return TradeHistoryDto.builder()
                    .historyId(history.getId())
                    .type(history.getType() != null ? history.getType().name() : null)
                    .changeAmount(history.getAmount())
                    .balanceSnapshot(history.getBalanceSnapshot())
                    .description(history.getDescription())
                    .datetime(history.getCreatedAt())
                    .build();
        }
    }
}