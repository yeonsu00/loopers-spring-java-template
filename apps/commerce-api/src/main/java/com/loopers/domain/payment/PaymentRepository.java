package com.loopers.domain.payment;

import java.util.Optional;

public interface PaymentRepository {
    void savePayment(Payment payment);

    Optional<Payment> findByOrderKey(String orderKey);

    Optional<Payment> findByTransactionKey(String transactionKey);
}

