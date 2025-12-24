package com.loopers.domain.ranking;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class RankingService {

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final int TTL_SECONDS = 2 * 24 * 60 * 60;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, String> redisTemplateMaster;

    public RankingService(@Qualifier("redisTemplateMaster") RedisTemplate<String, String> redisTemplateMaster) {
        this.redisTemplateMaster = redisTemplateMaster;
    }

    public void incrementScore(Long productId, RankingWeight weight) {
        LocalDate date = LocalDate.now();

        String key = getRankingKey(date);
        String member = String.valueOf(productId);

        ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
        Double newScore = zSetOps.incrementScore(key, member, weight.getWeight());

        redisTemplateMaster.expire(key, Duration.ofSeconds(TTL_SECONDS));

        log.debug("랭킹 점수 증가: productId={}, weight={}, date={}, newScore={}",
                productId, weight.getDescription(), date, newScore);
    }

    public void carryOverScore(LocalDate fromDate, LocalDate toDate) {
        String fromKey = getRankingKey(fromDate);
        String toKey = getRankingKey(toDate);

        ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

        var entries = zSetOps.reverseRangeWithScores(fromKey, 0, -1);

        if (entries == null || entries.isEmpty()) {
            log.info("carry-over할 데이터가 없음: fromDate={}", fromDate);
            return;
        }

        for (ZSetOperations.TypedTuple<String> entry : entries) {
            String productId = entry.getValue();
            Double score = entry.getScore();

            if (productId == null || score == null || score <= 0) {
                continue;
            }

            double carryOverScore = score * RankingWeight.CARRY_OVER.getWeight();
            zSetOps.incrementScore(toKey, productId, carryOverScore);
        }

        redisTemplateMaster.expire(toKey, java.time.Duration.ofSeconds(TTL_SECONDS));

        log.info("랭킹 점수 carry-over 완료: fromDate={}, toDate={}", fromDate, toDate);
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}

