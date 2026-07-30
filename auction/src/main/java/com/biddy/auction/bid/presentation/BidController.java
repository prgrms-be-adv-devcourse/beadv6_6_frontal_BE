package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.dto.PlaceBidCommand;
import com.biddy.auction.bid.application.dto.PlaceBidResult;
import com.biddy.auction.bid.application.usecase.BidUseCase;
import com.biddy.auction.bid.presentation.dto.PlaceBidRequest;
import com.biddy.auction.bid.presentation.dto.PlaceBidResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** sequence 및 멱등성 기반 서버 계산 입찰 API. */
@Tag(name = "입찰", description = "sequence 및 멱등성 기반 서버 계산 입찰 API")
@RestController
@RequestMapping("/api/v2/auctions/{auctionId}/bids")
@RequiredArgsConstructor
@Validated
public class BidController {

    private final BidUseCase bidUseCase;

    @Operation(summary = "서버 계산 입찰", description = "확인한 sequence와 금액 상한 안에서 서버가 다음 입찰가를 계산한다.")
    @PostMapping
    public ResponseEntity<PlaceBidResponse> placeBid(
            @Parameter(description = "경매 ID") @PathVariable String auctionId,
            @RequestHeader("X-Member-Id") @Positive Long bidderId,
            @RequestBody @Valid PlaceBidRequest request
    ) {
        PlaceBidCommand command = new PlaceBidCommand(
                auctionId,
                bidderId,
                request.requestId(),
                request.observedSequence(),
                request.maxAcceptableAmount()
        );
        PlaceBidResult result = bidUseCase.placeBid(command);
        HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(PlaceBidResponse.from(result));
    }
}
