package com.loopers.infrastructure.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PgPaymentDto {

    public record PgPaymentRequest(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("cardType") String cardType,
            @JsonProperty("cardNo") String cardNo,
            @JsonProperty("amount") Long amount,
            @JsonProperty("callbackUrl") String callbackUrl
    ) {
    }

    public record PgPaymentResponse(
            @JsonProperty("meta") Meta meta,
            @JsonProperty("data") TransactionData data
    ) {
        public record Meta(
                @JsonProperty("result") String result,
                @JsonProperty("errorCode") String errorCode,
                @JsonProperty("message") String message
        ) {
        }

        public record TransactionData(
                @JsonProperty("transactionKey") String transactionKey,
                @JsonProperty("status") String status,
                @JsonProperty("reason") String reason
        ) {
        }
    }
}


