package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Stock {

    @Column(nullable = false)
    private Integer quantity;

    public void reduceQuantity(Integer quantity) {
        if (this.quantity < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.quantity -= quantity;
    }
}
