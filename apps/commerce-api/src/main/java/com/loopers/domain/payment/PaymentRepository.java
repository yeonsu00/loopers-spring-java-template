package com.loopers.domain.payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment savePayment(Payment payment);

    Optional<Payment> findByOrderKey(String orderKey);

    Optional<Payment> findByTransactionKey(String transactionKey);
}

