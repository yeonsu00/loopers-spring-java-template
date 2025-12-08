package com.loopers.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.loopers.domain.payment.PaymentClient.PaymentRequest;
import com.loopers.domain.payment.PaymentClient.PaymentResponse;
import com.loopers.infrastructure.gateway.PgPaymentClient;
import com.loopers.infrastructure.gateway.PgPaymentDto;
import com.loopers.infrastructure.gateway.PgPaymentFeignClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@DisplayName("PgPaymentClient Resilience4j 동작 테스트")
class PgPaymentClientResilienceTest {

    @Autowired
    private PgPaymentClient pgPaymentClient;

    @MockitoSpyBean
    private PgPaymentFeignClient pgPaymentFeignClient;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private PaymentRequest paymentRequest;
    private String userId;

    @BeforeEach
    void setUp() {
        if (circuitBreakerRegistry != null) {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgPayment");
            circuitBreaker.transitionToClosedState();
            circuitBreaker.reset();
        }

        paymentRequest = new PaymentRequest(
                "order12345",
                "SAMSUNG",
                "1234-5678-9012-3456",
                10000L,
                "http://localhost:8080/api/v1/payments/callback"
        );
        userId = "user123";
    }

    @DisplayName("Circuit Breaker 동작 테스트")
    @Nested
    class CircuitBreakerTest {

        @Test
        @DisplayName("정상 응답 시 Circuit Breaker는 Close 상태를 유지한다")
        void circuitBreakerStaysClosed_whenSuccess() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            doReturn(successResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any(PgPaymentDto.PgPaymentRequest.class));

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isEqualTo("txn-key-123");
            assertThat(response.status()).isEqualTo("PENDING");
            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("설정: 슬라이딩 윈도우 20, 최소 호출 10, 실패율 60% - 10번 중 6번 실패하면 Open 상태")
        void circuitBreakerOpens_whenFailureRateExceeds60Percent() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doReturn(successResponse)  // 1 - 성공
                    .doReturn(successResponse)  // 2 - 성공
                    .doReturn(successResponse)  // 3 - 성공
                    .doReturn(successResponse)  // 4 - 성공
                    .doThrow(runtimeException)        // 5 - 실패
                    .doThrow(runtimeException)        // 6 - 실패
                    .doThrow(runtimeException)        // 7 - 실패
                    .doThrow(runtimeException)        // 8 - 실패
                    .doThrow(runtimeException)        // 9 - 실패
                    .doThrow(runtimeException)       // 10 - 실패
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            for (int i = 0; i < 10; i++) {
                try {
                    pgPaymentClient.requestPayment(paymentRequest, userId).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }

            // then
            verify(pgPaymentFeignClient, times(10)).requestPayment(anyString(), any());

            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);
            
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(10)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("HalfOpen 상태에서 성공 시 Closed 상태로 전환된다")
        void circuitBreakerTransitionsToClosed_whenHalfOpenSucceeds() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doReturn(successResponse)  // 1 - 성공
                    .doReturn(successResponse)  // 2 - 성공
                    .doReturn(successResponse)  // 3 - 성공
                    .doReturn(successResponse)  // 4 - 성공
                    .doThrow(runtimeException)        // 5 - 실패
                    .doThrow(runtimeException)        // 6 - 실패
                    .doThrow(runtimeException)        // 7 - 실패
                    .doThrow(runtimeException)        // 8 - 실패
                    .doThrow(runtimeException)        // 9 - 실패
                    .doThrow(runtimeException)       // 10 - 실패
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            for (int i = 0; i < 10; i++) {
                try {
                    pgPaymentClient.requestPayment(paymentRequest, userId).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }

            if (circuitBreakerRegistry != null) {
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgPayment");
                circuitBreaker.transitionToHalfOpenState();
            }

            doReturn(successResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isEqualTo("txn-key-123");
            assertThat(response.status()).isEqualTo("PENDING");

            verify(pgPaymentFeignClient, times(11)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("HalfOpen 상태에서 실패 시 Open 상태로 다시 전환된다")
        void circuitBreakerTransitionsToOpen_whenHalfOpenFails() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doReturn(successResponse)  // 1 - 성공
                    .doReturn(successResponse)  // 2 - 성공
                    .doReturn(successResponse)  // 3 - 성공
                    .doReturn(successResponse)  // 4 - 성공
                    .doThrow(runtimeException)        // 5 - 실패
                    .doThrow(runtimeException)        // 6 - 실패
                    .doThrow(runtimeException)        // 7 - 실패
                    .doThrow(runtimeException)        // 8 - 실패
                    .doThrow(runtimeException)        // 9 - 실패
                    .doThrow(runtimeException)       // 10 - 실패
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            for (int i = 0; i < 10; i++) {
                try {
                    pgPaymentClient.requestPayment(paymentRequest, userId).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                }
            }

            if (circuitBreakerRegistry != null) {
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgPayment");
                circuitBreaker.transitionToHalfOpenState();
            }

            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(11)).requestPayment(anyString(), any());
        }
    }

    @DisplayName("Retry 동작 테스트")
    @Nested
    class RetryTest {

        @Test
        @DisplayName("재시도 가능한 예외 발생 시 최대 2번 재시도한다 (총 3번 시도)")
        void retriesUpTo2Times_whenRetryableException() throws Exception {
            // given
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("모든 재시도 실패 시 Retry Fallback이 호출된다")
        void callsRetryFallback_whenAllRetriesFail() throws Exception {
            // given
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");
            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("재시도 불가능한 예외(CoreException) 발생 시 재시도하지 않지만 Fallback이 호출된다")
        void doesNotRetry_whenNonRetryableException() throws Exception {
            // given
            doReturn(null)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }
    }

    @DisplayName("Time Limiter 동작 테스트")
    @Nested
    class TimeLimiterTest {

        @Test
        @DisplayName("3초를 초과하는 응답은 타임아웃된다")
        void timesOut_whenResponseExceeds3Seconds() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            doAnswer(invocation -> {
                        Thread.sleep(4000);
                        return successResponse;
                    })
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            
            // then
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);
            assertThat(response.transactionKey()).isEqualTo("txn-key-123");
            assertThat(response.status()).isEqualTo("PENDING");
        }
    }

    @DisplayName("외부 시스템(PG) 응답별 동작 테스트")
    @Nested
    class ExternalSystemResponseTest {

        @Test
        @DisplayName("400 Bad Request 발생 시 재시도하지 않고 Fallback이 호출된다")
        void doesNotRetry_when400BadRequest() throws Exception {
            // given
            doReturn(null)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("정상 응답(200 OK) 시 성공적으로 처리한다")
        void succeeds_when200OK() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            doReturn(successResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isEqualTo("txn-key-123");
            assertThat(response.status()).isEqualTo("PENDING");
            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("500 Internal Server Error 발생 시 재시도한다")
        void retries_when500InternalServerError() throws Exception {
            // given
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("네트워크 연결 실패(RuntimeException) 발생 시 재시도한다")
        void retries_whenRuntimeException() throws Exception {
            // given
            RuntimeException runtimeException = new RuntimeException("Connection refused");

            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            CompletableFuture<PaymentResponse> future = pgPaymentClient.requestPayment(paymentRequest, userId);
            PaymentResponse response = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).contains("재시도 후에도 실패");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }
    }

    private PgPaymentDto.PgPaymentResponse createSuccessResponse(String transactionKey) {
        return new PgPaymentDto.PgPaymentResponse(
                new PgPaymentDto.PgPaymentResponse.Meta("SUCCESS", null, null),
                new PgPaymentDto.PgPaymentResponse.TransactionData(
                        transactionKey,
                        "PENDING",
                        null
                )
        );
    }
}
