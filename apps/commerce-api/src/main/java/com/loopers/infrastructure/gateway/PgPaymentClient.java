package com.loopers.infrastructure.gateway;

import com.loopers.domain.payment.PaymentClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
    public PaymentClient.PaymentResponse requestPayment(PaymentClient.PaymentRequest request) {

        PgPaymentDto.PgPaymentRequest pgRequest = new PgPaymentDto.PgPaymentRequest(
                request.orderId(),
                request.cardType(),
                request.cardNo(),
                (long) request.amount(),
                callbackUrl
        );

        PgPaymentDto.PgPaymentResponse response = pgPaymentFeignClient.requestPayment(request.userId(), pgRequest);

//        String errorCode = response.meta().errorCode();
//        if (errorCode != null && !errorCode.isBlank()) {
//            if (errorCode.equals("Bad Request")) {
//                throw new CoreException(ErrorType.BAD_REQUEST, response.meta().message());
//            }
//
//            if (errorCode.equals("Internal Server Error")) {
//                throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 시스템에 일시적인 오류가 발생했습니다.");
//            }
//        }

        return PaymentClient.PaymentResponse.createPaymentResponse(
                response.data().transactionKey(),
                response.data().status(),
                response.data().reason()
        );
    }

    public PaymentClient.PaymentResponse requestPaymentRetryFallback(PaymentClient.PaymentRequest request, Throwable t) {
        log.warn("PG 결제 요청 Retry Fallback 호출: orderKey={}, error={}", request.orderId(), t.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("결제 요청이 재시도 후에도 실패했습니다.");
    }

    public PaymentClient.PaymentResponse requestPaymentFallback(PaymentClient.PaymentRequest request, Exception e) {
        log.error("PG 결제 요청 CircuitBreaker Fallback 호출: orderKey={}, error={}", request.orderId(), e.getMessage());
        return PaymentClient.PaymentResponse.createFailResponse("PG 시스템이 일시적으로 사용할 수 없습니다.");
    }
}

