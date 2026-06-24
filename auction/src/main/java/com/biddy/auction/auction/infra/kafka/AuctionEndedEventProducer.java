package com.biddy.auction.auction.infra.kafka;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.bid.domain.model.Bid;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 경매 종료 Kafka Producer.
 *
 * <p>낙찰 시 {@code auction.ended} 토픽으로 이벤트를 발행하여
 * Order Service가 주문을 자동 생성하도록 한다.</p>
 *
 * <p>유찰(UNSOLD)은 주문 생성이 불필요하므로 이벤트를 발행하지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEndedEventProducer {

    private static final String TOPIC = "auction.ended";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 낙찰 이벤트를 발행한다.
     *
     * @param auction 종료된 경매
     * @param topBid  최고 입찰 (낙찰자)
     */
    public void publish(Auction auction, Bid topBid) {
        AuctionEndedEventPayload payload = AuctionEndedEventPayload.from(auction, topBid);

        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(TOPIC, auction.getAuctionId(), json);

            log.info("Kafka AUCTION_ENDED 발행: auctionId={}, winnerId={}, finalBid={}",
                    auction.getAuctionId(), topBid.getBidderId(), topBid.getAmount());
        } catch (JsonProcessingException e) {
            log.error("Kafka 이벤트 직렬화 실패: auctionId={}", auction.getAuctionId(), e);
        }
    }
}
