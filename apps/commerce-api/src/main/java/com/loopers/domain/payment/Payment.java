package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "payments")
@Getter
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String cardType;

    @Column(nullable = false)
    private String cardNumber;

    @Column
    private LocalDateTime paidAt;

    @Builder
    private Payment(Long orderId, PaymentStatus status, Integer amount, String cardType,
                    String cardNumber, LocalDateTime paidAt) {
        validateCreatePayment(orderId, status, amount, cardType, cardNumber);
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.cardType = cardType;
        this.cardNumber = cardNumber;
        this.paidAt = paidAt;
    }

    public Payment() {
    }

    public static Payment createPayment(Long orderId, Integer amount, String cardType,
                                       String cardNumber) {
        return Payment.builder()
                .orderId(orderId)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .cardType(cardType)
                .cardNumber(cardNumber)
                .paidAt(null)
                .build();
    }

    public void complete() {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 대기 상태인 경우에만 결제 완료 처리할 수 있습니다.");
        }
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }

    public void fail() {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 대기 상태인 경우에만 결제 실패 처리할 수 있습니다.");
        }
        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 완료 상태인 경우에만 결제 취소할 수 있습니다.");
        }
        this.status = PaymentStatus.CANCELED;
    }

    public void refund() {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 완료 상태인 경우에만 환불할 수 있습니다.");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    private static void validateCreatePayment(Long orderId, PaymentStatus status, Integer amount,
                                              String cardType, String cardNumber) {
        validateOrderId(orderId);
        validateStatus(status);
        validateAmount(amount);
        validateCardType(cardType);
        validateCardNumber(cardNumber);
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
        }
    }

    private static void validateStatus(PaymentStatus status) {
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 상태는 필수입니다.");
        }
    }

    private static void validateAmount(Integer amount) {
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 필수입니다.");
        }
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
    }

    private static void validateCardType(String cardType) {
        if (cardType == null || cardType.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 타입은 필수입니다.");
        }
    }

    private static void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 필수입니다.");
        }
    }
}

