package com.loopers.domain.coupon;

public interface DiscountPolicy {
    long calculateDiscountAmount(long originalPrice);
}

