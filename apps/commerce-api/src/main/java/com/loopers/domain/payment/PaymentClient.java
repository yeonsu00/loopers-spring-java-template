package com.loopers.domain.payment;

public interface PaymentClient {

    PaymentClient.PaymentResponse requestPayment(PaymentClient.PaymentRequest request);

    record PaymentRequest(
            String orderId,
            String cardType,
            String cardNo,
            Integer amount,
            String userId
    ) {
    }

    record PaymentResponse(
            String transactionKey,
            String status,
            String reason
    ) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }
}



