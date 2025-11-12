package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "number", column = @Column(name = "order_number", nullable = false, unique = true))
    })
    private OrderNumber orderNumber;

    private Long userId;

    private Integer totalPrice;

    private OrderStatus orderStatus;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "receiverName", column = @Column(name = "receiver_name", nullable = false, unique = true)),
            @AttributeOverride(name = "receiverPhoneNumber", column = @Column(name = "phone_number", nullable = false, unique = true)),
            @AttributeOverride(name = "baseAddress", column = @Column(name = "base_address", nullable = false, unique = true)),
            @AttributeOverride(name = "detailAddress", column = @Column(name = "detail_address", nullable = false, unique = true))
    })
    private Delivery delivery;

    private LocalDateTime orderedAt;



}
