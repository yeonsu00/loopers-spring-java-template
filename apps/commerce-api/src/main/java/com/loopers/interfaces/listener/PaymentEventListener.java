package com.loopers.interfaces.listener;

import com.loopers.application.order.OrderEvent;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class PaymentEventListener {

    private final PaymentService paymentService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderEvent.OrderCreated event) {
        paymentService.createPayment(event.originalTotalPrice() - event.discountPrice(), event.orderKey());
    }

}

