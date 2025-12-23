package com.loopers.interfaces.api.ranking;

import java.util.List;

public class RankingV1Dto {

    public record DailyRankingListResponse(
            List<DailyRankingItem> rankings
    ) {
        public static DailyRankingListResponse from(List<DailyRankingItem> items) {
            return new DailyRankingListResponse(items);
        }
    }

    public record DailyRankingItem(
            Long productId,
            String productName,
            String brandName,
            Integer price,
            Integer likeCount,
            Integer rank
    ) {
    }

}
