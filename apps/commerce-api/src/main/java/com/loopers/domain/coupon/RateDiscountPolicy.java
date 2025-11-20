package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public class RateDiscountPolicy implements DiscountPolicy {

    private final double rate;

    public RateDiscountPolicy(double rate) {
        validate(rate);
        this.rate = rate;
    }

    private void validate(double rate) {
        if (rate < 0 || rate > 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인율은 0 이상 1 이하여야 합니다.");
        }
    }

    @Override
    public long calculateDiscountAmount(long originalPrice) {
        if (originalPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "원가는 0 이상이어야 합니다.");
        }

        long discount = (long) (originalPrice * rate);
        return Math.min(discount, originalPrice);
    }
}

