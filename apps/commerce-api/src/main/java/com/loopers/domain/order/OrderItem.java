package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "order_items")
@Getter
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long productId;

    private String productName;

    private Integer price;

    private Integer quantity;

    @Builder
    private OrderItem(Order order, Long productId, String productName, Integer price, Integer quantity) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public OrderItem() {
    }

    public static OrderItem createOrderItem(Order order, Long productId, String productName, Integer price, Integer quantity) {
        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
    }
}
