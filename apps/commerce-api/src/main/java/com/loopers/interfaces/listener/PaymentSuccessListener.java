package com.loopers.interfaces.listener;

import com.loopers.application.payment.PaymentEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentSuccessListener {

    private final OrderService orderService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handle(PaymentEvent.PaymentCompleted event) {
        log.info("PaymentCompleted 이벤트 수신 - 주문 결제 완료 처리: orderKey={}, transactionKey={}, amount={}",
                event.orderKey(), event.transactionKey(), event.amount());

        Order order = orderService.getOrderByOrderKey(event.orderKey());
        orderService.payOrder(order);
        orderService.saveOrder(order);
    }
}

