package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller implements RankingV1ApiSpec {

    @GetMapping
    @Override
    public ApiResponse<RankingV1Dto.DailyRankingListResponse> getDailyRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        RankingV1Dto.DailyRankingListResponse response =
                new RankingV1Dto.DailyRankingListResponse(List.of());

        return ApiResponse.success(response);
    }

}
