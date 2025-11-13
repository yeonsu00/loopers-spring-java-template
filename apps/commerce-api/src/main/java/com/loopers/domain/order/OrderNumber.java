package com.loopers.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderNumber {

    @Column(nullable = false)
    private String number;

    private OrderNumber(String number) {
        this.number = number;
    }

    public static OrderNumber generate() {
        return new OrderNumber("");
    }
}
