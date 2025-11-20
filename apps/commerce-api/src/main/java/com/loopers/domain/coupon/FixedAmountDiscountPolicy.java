package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public class FixedAmountDiscountPolicy implements DiscountPolicy {

    private final long amount;

    public FixedAmountDiscountPolicy(long amount) {
        validate(amount);
        this.amount = amount;
    }

    private void validate(long amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.");
        }
    }

    @Override
    public long calculateDiscountAmount(long originalPrice) {
        if (originalPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "원가는 0 이상이어야 합니다.");
        }
        return Math.min(originalPrice, amount);
    }
}

