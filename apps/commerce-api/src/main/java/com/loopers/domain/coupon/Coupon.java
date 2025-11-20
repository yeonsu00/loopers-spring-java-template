package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Discount discount;

    @Column(nullable = false)
    private Boolean used;

    @Builder
    private Coupon(Long userId, String name, Discount discount) {
        validate(userId, name, discount);
        this.userId = userId;
        this.name = name;
        this.discount = discount;
        this.used = false;
    }

    private void validate(Long userId, String name, Discount discount) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 필수입니다.");
        }
        if (discount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 정책 정보는 필수입니다.");
        }
    }

    public long calculateFinalPrice(long orderPrice) {
        if (orderPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 금액은 0 이상이어야 합니다.");
        }
        DiscountPolicy policy = discount.toPolicy();
        long discountAmount = policy.calculateDiscountAmount(orderPrice);
        return orderPrice - discountAmount;
    }

    public long calculateDiscountAmount(long orderPrice) {
        if (orderPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 금액은 0 이상이어야 합니다.");
        }
        DiscountPolicy policy = discount.toPolicy();
        return policy.calculateDiscountAmount(orderPrice);
    }

    public void use() {
        if (used) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        this.used = true;
    }

    public boolean isUsed() {
        return used;
    }

    public static Coupon createCoupon(Long userId, String name, Discount discount) {
        return Coupon.builder()
                .userId(userId)
                .name(name)
                .discount(discount)
                .build();
    }
}
