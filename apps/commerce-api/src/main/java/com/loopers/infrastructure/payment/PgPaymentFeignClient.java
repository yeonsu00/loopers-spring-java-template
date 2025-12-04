package com.loopers.infrastructure.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "pgPaymentClient",
        url = "${pg-simulator.url:http://localhost:8081}",
        path = "/api/v1/payments",
        configuration = PaymentClientConfig.class
)
public interface PgPaymentFeignClient {

    @PostMapping
    PgPaymentDto.PgPaymentResponse requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgPaymentDto.PgPaymentRequest request
    );
}

