package com.loopers.application.kafka;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.ZonedDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface KafkaEvent {
    String eventId();
    ZonedDateTime timestamp();

    static String generateEventId(String eventType, String aggregateKey) {
        return String.format("%s-%s-%d", eventType, aggregateKey, System.currentTimeMillis());
    }

    sealed interface OrderEvent extends KafkaEvent permits
            OrderEvent.OrderCreated,
            OrderEvent.OrderPaid {

        String orderKey();
        Long userId();
        Long orderId();

        record OrderCreated(
                String eventId,
                String orderKey,
                Long userId,
                Long orderId,
                Integer totalPrice,
                Integer discountPrice,
                ZonedDateTime timestamp
        ) implements OrderEvent {
            public static OrderCreated from(String orderKey, Long userId, Long orderId, Integer totalPrice, Integer discountPrice) {
                return new OrderCreated(
                        KafkaEvent.generateEventId("order-created", orderKey),
                        orderKey,
                        userId,
                        orderId,
                        totalPrice,
                        discountPrice,
                        ZonedDateTime.now()
                );
            }
        }

        record OrderPaid(
                String eventId,
                String orderKey,
                Long userId,
                Long orderId,
                Integer totalPrice,
                ZonedDateTime timestamp
        ) implements OrderEvent {
            public static OrderPaid from(String orderKey, Long userId, Long orderId, Integer totalPrice) {
                return new OrderPaid(
                        KafkaEvent.generateEventId("order-paid", orderKey),
                        orderKey,
                        userId,
                        orderId,
                        totalPrice,
                        ZonedDateTime.now()
                );
            }
        }
    }

    sealed interface ProductEvent extends KafkaEvent permits
            ProductEvent.ProductLiked,
            ProductEvent.ProductUnliked,
            ProductEvent.ProductViewed,
            ProductEvent.ProductStockDepleted {

        Long productId();

        record ProductLiked(
                String eventId,
                Long productId,
                Long userId,
                ZonedDateTime timestamp
        ) implements ProductEvent {
            public static ProductLiked from(Long productId, Long userId) {
                return new ProductLiked(
                        KafkaEvent.generateEventId("product-liked", productId.toString()),
                        productId,
                        userId,
                        ZonedDateTime.now()
                );
            }
        }

        record ProductUnliked(
                String eventId,
                Long productId,
                Long userId,
                ZonedDateTime timestamp
        ) implements ProductEvent {
            public static ProductUnliked from(Long productId, Long userId) {
                return new ProductUnliked(
                        KafkaEvent.generateEventId("product-unliked", productId.toString()),
                        productId,
                        userId,
                        ZonedDateTime.now()
                );
            }
        }

        record ProductViewed(
                String eventId,
                Long productId,
                Long userId,
                ZonedDateTime timestamp
        ) implements ProductEvent {
            public static ProductViewed from(Long productId, Long userId) {
                return new ProductViewed(
                        KafkaEvent.generateEventId("product-viewed", productId.toString()),
                        productId,
                        userId,
                        ZonedDateTime.now()
                );
            }
        }

        record ProductStockDepleted(
                String eventId,
                Long productId,
                Integer remainingStock,
                ZonedDateTime timestamp
        ) implements ProductEvent {
            public static ProductStockDepleted from(Long productId, Integer remainingStock) {
                return new ProductStockDepleted(
                        KafkaEvent.generateEventId("product-stock-depleted", productId.toString()),
                        productId,
                        remainingStock,
                        ZonedDateTime.now()
                );
            }
        }
    }
}
