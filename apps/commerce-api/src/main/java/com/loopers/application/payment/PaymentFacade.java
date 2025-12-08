package com.loopers.application.payment;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final PointService pointService;
    private final CouponService couponService;

    @Transactional
    public PaymentInfo requestPayment(PaymentCommand.RequestPaymentCommand command) {
        Payment savedPayment = paymentService.getPaymentByOrderKey(command.orderKey());
        paymentService.validatePaymentStatus(savedPayment);

        User user = userService.getUserByLoginId(command.loginId());
        Order order = orderService.getOrderByOrderKey(command.orderKey());
        orderService.validateIsUserOrder(command.orderKey(), user.getId());

        try {
            Payment updatedPayment = paymentService.requestPaymentToPg(
                    savedPayment,
                    command.cardType(),
                    command.cardNo(),
                    command.loginId()
            );

            return PaymentInfo.from(updatedPayment);
        } catch (Exception e) {
            log.error("PG 결제 요청 실패: orderKey={}, error={}", command.orderKey(), e.getMessage(), e);
            handleFailure(savedPayment, order, "PG 결제 요청 실패: " + e.getMessage());
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 요청에 실패했습니다.");
        }
    }

    @Transactional
    public void handlePaymentCallback(PaymentV1Dto.PaymentCallbackRequest request) {
        Payment payment = paymentService.getPendingPaymentByTransactionKey(request.transactionKey());
        Order order = orderService.getOrderByOrderKey(payment.getOrderKey());

        String status = request.status();
        switch (status) {
            case "SUCCESS" -> handleSuccess(payment, order);
            case "FAILED" -> handleFailure(payment, order, request.reason());
            default -> {
                log.warn("알 수 없는 결제 상태: transactionKey={}, status={}", request.transactionKey(), status);
            }
        }
    }

    private void handleSuccess(Payment payment, Order order) {
        orderService.payOrder(order);
        paymentService.updatePaymentStatus(payment.getTransactionKey(), PaymentStatus.COMPLETED);

        log.info("결제 완료 처리: orderId={}, transactionKey={}", order.getId(), payment.getTransactionKey());
    }

    private void handleFailure(Payment payment, Order order, String reason) {
        orderService.cancelOrder(order);
        
        if (payment.getTransactionKey() != null && !payment.getTransactionKey().isBlank()) {
            paymentService.updatePaymentStatus(payment.getTransactionKey(), PaymentStatus.FAILED);
        } else {
            paymentService.updatePaymentStatusByOrderKey(payment.getOrderKey(), PaymentStatus.FAILED);
        }

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = productService.findProductById(orderItem.getProductId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: " + orderItem.getProductId()));
            productService.restoreStock(product, orderItem.getQuantity());
        }

        int pointAmount = order.getOriginalTotalPrice() - (order.getDiscountPrice() != null ? order.getDiscountPrice() : 0);
        pointService.restorePoint(order.getUserId(), pointAmount);

        if (order.getCouponId() != null) {
            Coupon coupon = couponService.getCouponByIdAndUserId(order.getCouponId(), order.getUserId());
            couponService.restoreCoupon(coupon);
        }

        log.warn("결제 실패 처리: orderKey={}, transactionKey={}, reason={}",
                payment.getOrderKey(), payment.getTransactionKey(), reason);
    }
}



