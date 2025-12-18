package com.loopers.interfaces.listener;

import com.loopers.application.kafka.KafkaEvent;
import com.loopers.application.like.LikeEvent;
import com.loopers.application.order.OrderEvent;
import com.loopers.application.product.ProductEvent;
import com.loopers.application.userbehavior.UserBehaviorEvent;
import com.loopers.domain.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaOutboxEventListener {

    private final OutboxService outboxService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderEvent.OrderCreated event) {
        KafkaEvent.OrderCreated kafkaEvent = KafkaEvent.OrderCreated.from(
                event.orderKey(),
                event.userId(),
                event.orderId(),
                event.originalTotalPrice(),
                event.discountPrice()
        );
        outboxService.saveOutbox("order-created-events", event.orderKey(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderEvent.OrderPaid event) {
        KafkaEvent.OrderPaid kafkaEvent = KafkaEvent.OrderPaid.from(
                event.orderKey(),
                event.userId(),
                event.orderId(),
                event.totalPrice()
        );
        outboxService.saveOutbox("order-paid-events", event.orderKey(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeRecorded(LikeEvent.LikeRecorded event) {
        KafkaEvent.ProductLiked kafkaEvent = KafkaEvent.ProductLiked.from(
                event.productId(),
                event.userId()
        );
        outboxService.saveOutbox("product-liked-events", event.productId().toString(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeCancelled(LikeEvent.LikeCancelled event) {
        KafkaEvent.ProductUnliked kafkaEvent = KafkaEvent.ProductUnliked.from(
                event.productId(),
                event.userId()
        );
        outboxService.saveOutbox("product-unliked-events", event.productId().toString(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductViewed(UserBehaviorEvent.ProductViewed event) {
        KafkaEvent.ProductViewed kafkaEvent = KafkaEvent.ProductViewed.from(
                event.productId(),
                event.userId()
        );
        outboxService.saveOutbox("product-viewed-events", event.productId().toString(), kafkaEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockDepleted(ProductEvent.StockDepleted event) {
        KafkaEvent.ProductStockDepleted kafkaEvent = KafkaEvent.ProductStockDepleted.from(
                event.productId(),
                event.remainingStock()
        );
        outboxService.saveOutbox("product-stock-depleted-events", event.productId().toString(), kafkaEvent);
    }
}

