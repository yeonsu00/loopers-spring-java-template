package com.loopers.domain.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public void createPayment(Long orderId, Integer amount, String orderKey) {
        Payment payment = Payment.createPayment(orderId, amount, orderKey);
        paymentRepository.savePayment(payment);
    }
}

