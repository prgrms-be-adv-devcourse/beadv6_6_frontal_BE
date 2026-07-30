package com.biddy.auction.bid.infra.kafka;

import com.biddy.auction.auction.domain.model.Auction;
import com.biddy.auction.bid.domain.model.Bid;
import com.biddy.auction.outbox.domain.OutboxEvent;
import com.biddy.auction.outbox.domain.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 성공 입찰 이벤트를 현재 입찰 트랜잭션의 Outbox에 기록한다. */
@Component
@RequiredArgsConstructor
@Slf4j
public class BidAcceptedOutboxWriter {

    public static final String TOPIC = "auction.bid.accepted";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEvent save(Auction auction, Bid bid) {
        UUID eventId = UUID.randomUUID();
        BidAcceptedEventPayload eventPayload = BidAcceptedEventPayload.from(eventId, auction, bid);

        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateType("BID")
                    .aggregateId(auction.getAuctionId())
                    .topic(TOPIC)
                    .payload(objectMapper.writeValueAsString(eventPayload))
                    .build();
            OutboxEvent savedEvent = outboxEventRepository.save(outboxEvent);
            log.debug("BID_ACCEPTED Outbox 저장 - eventId: {}, auctionId: {}, sequence: {}",
                    eventId, auction.getAuctionId(), bid.getSequence());
            return savedEvent;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("입찰 성공 이벤트 직렬화에 실패했습니다.", exception);
        }
    }
}
