package com.loopers.infrastructure.gateway;

import com.loopers.domain.payment.PaymentClient;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgPaymentClient implements PaymentClient {

    @Value("${pg-simulator.callback-url}")
    private String callbackUrl;

    private final PgPaymentFeignClient pgPaymentFeignClient;

    @Override
    @Retry(name = "pgRetry", fallbackMethod = "requestPaymentRetryFallback")
    @CircuitBreaker(name = "pgPayment", fallbackMethod = "requestPaymentFallback")
    @TimeLimiter(name = "pgPayment")
    public PaymentClient.PaymentResponse requestPayment(PaymentClient.PaymentRequest request) {

        PgPaymentDto.PgPaymentRequest pgRequest = new PgPaymentDto.PgPaymentRequest(
                request.orderId(),
                request.cardType(),
                request.cardNo(),
                (long) request.amount(),
                callbackUrl
        );

        PgPaymentDto.PgPaymentResponse response = pgPaymentFeignClient.requestPayment(request.userId(), pgRequest);

        String errorCode = response.meta().errorCode();
        if (errorCode != null && !errorCode.isBlank()) {
            if (errorCode.equals("Bad Request")) {
                throw new CoreException(ErrorType.BAD_REQUEST, response.meta().message());
            }

            if (errorCode.equals("Internal Server Error")) {
                throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 시스템에 일시적인 오류가 발생했습니다.");
            }
        }

        return new PaymentResponse(
                response.data().transactionKey(),
                response.data().status(),
                response.data().reason()
        );
    }

    public CompletableFuture<PaymentResponse> requestPaymentRetryFallback(
            PaymentRequest request, String userId, Throwable t) {
        log.warn("PG 결제 요청 Retry Fallback 호출: orderKey={}, userId={}, error={}", request.orderId(), userId, t.getMessage());

        PaymentResponse response = new PaymentResponse(
                null,
                "PENDING",
                "결제 요청이 재시도 후에도 실패했습니다. 잠시 후 다시 시도해주세요."
        );
        return CompletableFuture.completedFuture(response);
    }

    public CompletableFuture<PaymentResponse> requestPaymentFallback(
            PaymentRequest request, String userId, Exception e) {
        log.error("PG 결제 요청 CircuitBreaker Fallback 호출: orderKey={}, userId={}, error={}", request.orderId(), userId, e.getMessage());
        throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 시스템이 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}

