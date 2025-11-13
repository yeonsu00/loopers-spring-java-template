package com.loopers.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String receiverPhoneNumber;

    @Column(nullable = false)
    private String baseAddress;

    @Column(nullable = false)
    private String detailAddress;

    @Builder
    private Delivery(String receiverName, String receiverPhoneNumber, String baseAddress, String detailAddress) {
        this.receiverName = receiverName;
        this.receiverPhoneNumber = receiverPhoneNumber;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }

    public static Delivery createDelivery(String receiverName, String receiverPhoneNumber, String baseAddress,
                                          String detailAddress) {
        return Delivery.builder()
                .receiverName(receiverName)
                .receiverPhoneNumber(receiverPhoneNumber)
                .baseAddress(baseAddress)
                .detailAddress(detailAddress)
                .build();
    }
}
