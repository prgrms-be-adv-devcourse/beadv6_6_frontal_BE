package com.biddy.auction.bid.presentation;

import com.biddy.auction.bid.application.dto.PlaceBidV2Command;
import com.biddy.auction.bid.application.dto.PlaceBidV2Result;
import com.biddy.auction.bid.application.usecase.BidV2UseCase;
import com.biddy.auction.bid.presentation.dto.PlaceBidV2Request;
import com.biddy.auction.bid.presentation.dto.PlaceBidV2Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기능 플래그로 단계적으로 노출하는 서버 계산 입찰 API v2. */
@Tag(name = "입찰 v2", description = "sequence 및 멱등성 기반 서버 계산 입찰 API")
@RestController
@RequestMapping("/api/v2/auctions/{auctionId}/bids")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "bid.api-v2", name = "enabled", havingValue = "true")
public class BidV2Controller {

    private final BidV2UseCase bidV2UseCase;

    @Operation(summary = "서버 계산 입찰", description = "확인한 sequence와 금액 상한 안에서 서버가 다음 입찰가를 계산한다.")
    @PostMapping
    public ResponseEntity<PlaceBidV2Response> placeBid(
            @Parameter(description = "경매 ID") @PathVariable String auctionId,
            @RequestHeader("X-Member-Id") @Positive Long bidderId,
            @RequestBody @Valid PlaceBidV2Request request
    ) {
        PlaceBidV2Command command = new PlaceBidV2Command(
                auctionId,
                bidderId,
                request.requestId(),
                request.observedSequence(),
                request.maxAcceptableAmount()
        );
        PlaceBidV2Result result = bidV2UseCase.placeBid(command);
        HttpStatus status = result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(PlaceBidV2Response.from(result));
    }
}
