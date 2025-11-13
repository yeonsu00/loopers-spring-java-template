package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {

    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    List<OrderItem> orderItems = new ArrayList<>();

    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "receiverName", column = @Column(name = "receiver_name", nullable = false, unique = true)),
            @AttributeOverride(name = "receiverPhoneNumber", column = @Column(name = "phone_number", nullable = false, unique = true)),
            @AttributeOverride(name = "baseAddress", column = @Column(name = "base_address", nullable = false, unique = true)),
            @AttributeOverride(name = "detailAddress", column = @Column(name = "detail_address", nullable = false, unique = true))
    })
    private Delivery delivery;

    public Order() {
    }

    @Builder
    private Order(Long userId, List<OrderItem> orderItems, Integer totalPrice,
                 OrderStatus orderStatus, Delivery delivery) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalPrice = totalPrice;
        this.orderStatus = orderStatus;
        this.delivery = delivery;
    }

    public static Order createOrder(Long userId, Delivery delivery) {
        return Order.builder()
                .userId(userId)
                .totalPrice(0)
                .orderStatus(OrderStatus.CREATED)
                .delivery(delivery)
                .build();
    }

    public void addOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상품은 필수입니다.");
        }
        orderItems.add(orderItem);
    }

    public void addPrice(int price) {
        this.totalPrice += price;
    }
}
