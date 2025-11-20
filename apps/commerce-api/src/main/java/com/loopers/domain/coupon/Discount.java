package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Discount {

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private PolicyType type;

    @Column(name = "discount_amount")
    private Long discountAmount;

    @Column(name = "discount_rate")
    private Double discountRate;

    public static Discount createFixed(long amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.");
        }
        Discount info = new Discount();
        info.type = PolicyType.FIXED;
        info.discountAmount = amount;
        info.discountRate = null;
        return info;
    }

    public static Discount createPercent(double rate) {
        if (rate < 0 || rate > 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인율은 0 이상 1 이하여야 합니다.");
        }
        Discount info = new Discount();
        info.type = PolicyType.PERCENT;
        info.discountRate = rate;
        info.discountAmount = null;
        return info;
    }

    public DiscountPolicy toPolicy() {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 정책 타입은 필수입니다.");
        }

        return switch (type) {
            case FIXED -> {
                if (discountAmount == null) {
                    throw new CoreException(ErrorType.BAD_REQUEST, "정액 할인 정책의 할인 금액은 필수입니다.");
                }
                yield new FixedAmountDiscountPolicy(discountAmount);
            }
            case PERCENT -> {
                if (discountRate == null) {
                    throw new CoreException(ErrorType.BAD_REQUEST, "정률 할인 정책의 할인율은 필수입니다.");
                }
                yield new RateDiscountPolicy(discountRate);
            }
        };
    }
}

