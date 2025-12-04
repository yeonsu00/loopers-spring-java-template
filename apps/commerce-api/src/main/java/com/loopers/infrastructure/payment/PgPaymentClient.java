package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentClient;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgPaymentClient implements PaymentClient {

    private final PgPaymentFeignClient pgPaymentFeignClient;

    @Override
    @Retry(name = "pgRetry", fallbackMethod = "requestPaymentRetryFallback")
    @CircuitBreaker(name = "pgPayment", fallbackMethod = "requestPaymentFallback")
    @TimeLimiter(name = "pgPayment")
    public CompletableFuture<PaymentResponse> requestPayment(PaymentRequest request, String userId) {
        try {
            log.info("PG 결제 요청: orderKey={}, amount={}", request.orderId(), request.amount());

            PgPaymentDto.PgPaymentRequest pgRequest = new PgPaymentDto.PgPaymentRequest(
                    request.orderId(),
                    request.cardType(),
                    request.cardNo(),
                    request.amount(),
                    request.callbackUrl()
            );

            PgPaymentDto.PgPaymentResponse response = pgPaymentFeignClient.requestPayment(userId, pgRequest);

            if (response != null && response.data() != null) {
                log.info("PG 결제 요청 성공: transactionKey={}, status={}",
                        response.data().transactionKey(),
                        response.data().status());

                PaymentResponse paymentResponse = new PaymentResponse(
                        response.data().transactionKey(),
                        response.data().status(),
                        response.data().reason()
                );
                return CompletableFuture.completedFuture(paymentResponse);
            } else {
                log.error("PG 결제 요청 실패: 응답이 null입니다.");
                throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청이 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("PG 결제 요청 중 오류 발생: {}", e.getMessage(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청 중 오류가 발생했습니다: " + e.getMessage());
        }
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

