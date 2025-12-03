package com.loopers.infrastructure.payment;

import feign.Request;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Client 타임아웃 설정
 * - connectTimeout: 연결 타임아웃 (ms)
 * - readTimeout: 응답 읽기 타임아웃 (ms)
 * 실무에서는 보통 2~5초 사이로 설정하며, 기능 특성과 요청 수에 따라 조절합니다.
 */
@Configuration
public class PaymentClientConfig {

    @Bean
    public Request.Options feignOptions() {
        // 연결 타임아웃: 1초, 응답 읽기 타임아웃: 2초, 리디렉션 허용
        return new Request.Options(
                1000, TimeUnit.MILLISECONDS,
                2000, TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    public Retryer retryer() {
        // 재시도 설정: 100ms 간격으로 시작, 최대 1초까지 증가, 최대 3회 시도
        return new Retryer.Default(
                100,
                1000,
                2
        );
    }
}

