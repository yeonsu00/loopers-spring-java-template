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

        public static PaymentResponse createPaymentResponse(String transactionKey, String status, String reason) {
            return new PaymentResponse(transactionKey, status, reason);
        }

        public static PaymentResponse createFailResponse(String reason) {
            return new PaymentResponse(null, "FAILED", reason);
        }
    }
}
