package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.dto.BidHistoryQuery;
import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
import com.biddy.auction.bid.application.usecase.BidQueryUseCase;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.bid.presentation.dto.BidHistoryResponse;
import com.biddy.auction.bid.presentation.dto.LegacyPlaceBidRequest;
import com.biddy.auction.bid.presentation.dto.LegacyPlaceBidResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 기존 프론트엔드를 위한 v1 입찰 호환 및 내역 조회 Controller.
 */
@Tag(name = "입찰 v1", description = "기존 프론트엔드 호환 입찰 및 입찰 내역 조회 API")
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
@RequiredArgsConstructor
@Validated
public class BidV1CompatibilityController {

    private final BidUseCase bidUseCase;
    private final BidQueryUseCase bidQueryUseCase;

    @Operation(summary = "v1 호환 입찰", description = "기존 amount를 상한으로 사용해 서버가 다음 입찰가를 계산한다.")
    @PostMapping
    public ResponseEntity<LegacyPlaceBidResponse> placeBid(
            @Parameter(description = "경매 ID") @PathVariable String auctionId,
            @RequestHeader("X-Member-Id") @Positive Long bidderId,
            @RequestBody @Valid LegacyPlaceBidRequest request
    ) {
        PlaceBidCommand command = PlaceBidCommand.compatibleV1(
                auctionId, bidderId, request.amount()
        );
        PlaceBidResult result = bidUseCase.placeBid(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(LegacyPlaceBidResponse.from(result));
    }

    @Operation(summary = "입찰 내역 조회", description = "특정 경매의 입찰 내역을 최신순으로 페이징 조회한다.")
    @GetMapping
    public ResponseEntity<Page<BidHistoryResponse>> getBidHistory(
            @Parameter(description = "경매 ID") @PathVariable String auctionId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        BidHistoryQuery query = new BidHistoryQuery(auctionId, page, size);
        Page<BidHistoryResponse> response = bidQueryUseCase.getBidHistory(query)
                .map(BidHistoryResponse::from);
        return ResponseEntity.ok(response);
    }
}
