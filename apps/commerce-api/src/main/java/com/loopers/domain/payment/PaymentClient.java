package com.loopers.domain.payment;

import java.util.concurrent.CompletableFuture;

public interface PaymentClient {

    CompletableFuture<PaymentResponse> requestPayment(PaymentRequest request, String userId);

    record PaymentRequest(
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {
    }

    record PaymentResponse(
            String transactionKey,
            String status,
            String reason
    ) {
    }
}



