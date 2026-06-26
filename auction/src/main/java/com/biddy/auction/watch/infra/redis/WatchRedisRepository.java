package com.biddy.auction.watch.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;


/**
 * 관심 경매 Redis Repository.
 *
 * <p>Redis SET과 Counter를 활용하여 관심 경매 조회를 O(1)로 처리한다.</p>
 *
 * <p>Key 설계:
 * <ul>
 *   <li>{@code watch:user:{memberId}} — 회원별 관심 경매 ID SET</li>
 *   <li>{@code watch:auction:{auctionId}:count} — 경매별 관심 등록 수 Counter</li>
 * </ul></p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class WatchRedisRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String USER_KEY_PREFIX = "watch:user:";
    private static final String COUNT_KEY_PREFIX = "watch:auction:";
    private static final String COUNT_KEY_SUFFIX = ":count";

    // ─── 회원별 관심 SET 조작 ───

    /** 관심 등록 (SET에 auctionId 추가) */
    public void addWatch(Long memberId, String auctionId) {
        redisTemplate.opsForSet().add(userKey(memberId), auctionId);
    }

    /** 관심 해제 (SET에서 auctionId 제거) */
    public void removeWatch(Long memberId, String auctionId) {
        redisTemplate.opsForSet().remove(userKey(memberId), auctionId);
    }

    /** 관심 여부 확인 — O(1) */
    public boolean isWatching(Long memberId, String auctionId) {
        Boolean result = redisTemplate.opsForSet().isMember(userKey(memberId), auctionId);
        return Boolean.TRUE.equals(result);
    }

    // ─── 경매별 관심 수 Counter ───

    /** 관심 수 +1 */
    public Long incrementCount(String auctionId) {
        return redisTemplate.opsForValue().increment(countKey(auctionId));
    }

    /** 관심 수 -1 (0 미만 방지) */
    public Long decrementCount(String auctionId) {
        Long count = redisTemplate.opsForValue().decrement(countKey(auctionId));
        if (count != null && count < 0) {
            redisTemplate.opsForValue().set(countKey(auctionId), "0");
            return 0L;
        }
        return count != null ? count : 0L;
    }

    /** 관심 수 조회 */
    public int getCount(String auctionId) {
        String val = redisTemplate.opsForValue().get(countKey(auctionId));
        return val != null ? Integer.parseInt(val) : 0;
    }

    /** 관심 수 설정 (Warm-up용) */
    public void setCount(String auctionId, int count) {
        redisTemplate.opsForValue().set(countKey(auctionId), String.valueOf(count));
    }

    // ─── Key 헬퍼 ───

    private String userKey(Long memberId) {
        return USER_KEY_PREFIX + memberId;
    }

    private String countKey(String auctionId) {
        return COUNT_KEY_PREFIX + auctionId + COUNT_KEY_SUFFIX;
    }
}
