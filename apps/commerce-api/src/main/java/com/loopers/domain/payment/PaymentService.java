package com.loopers.domain.payment;

import com.loopers.domain.payment.PaymentClient.PaymentRequest;
import com.loopers.domain.payment.PaymentClient.PaymentResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentClient paymentClient;

    @Value("${server.port:8080}")
    private String serverPort;

    public void createPayment(Integer amount, String orderKey) {
        Payment payment = Payment.createPayment(amount, orderKey);
        paymentRepository.savePayment(payment);
    }

    public Payment getPaymentByOrderKey(String orderKey) {
        return paymentRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    public void validatePaymentStatus(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                throw new CoreException(ErrorType.CONFLICT, "이미 성공한 결제입니다.");
            }
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 대기 상태인 경우에만 결제 요청할 수 있습니다. 현재 상태: " + payment.getStatus());
        }
    }

    @Transactional
    public Payment requestPaymentToPg(Payment savedPayment, String cardType, String cardNo, String userId) {
        String callbackUrl = String.format("http://localhost:%s/api/v1/payments/callback", serverPort);
        PaymentRequest request = new PaymentRequest(
                savedPayment.getOrderKey(),
                cardType,
                cardNo,
                (long) savedPayment.getAmount(),
                callbackUrl
        );

        try {
            CompletableFuture<PaymentResponse> paymentFuture = paymentClient.requestPayment(request, userId);
            PaymentResponse response = paymentFuture.get();
            String transactionKey = response.transactionKey();

            if (transactionKey != null && !transactionKey.isBlank()) {
                savedPayment.updateTransactionKey(transactionKey);
                savedPayment.updateStatus(PaymentStatus.COMPLETED);
                savedPayment.updateCard(cardType, cardNo);
                paymentRepository.savePayment(savedPayment);

                log.info("PG 결제 요청 완료: orderKey={}, transactionKey={}", savedPayment.getOrderKey(), transactionKey);
                return savedPayment;
            } else {
                log.error("PG 결제 요청 실패: transactionKey가 null입니다.");
                throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 요청에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("PG 결제 요청 실패: orderKey={}, error={}", savedPayment.getOrderKey(), e.getMessage(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 요청에 실패했습니다.");
        }
    }

    public Payment getPendingPaymentByTransactionKey(String transactionKey) {
        Payment payment = paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new CoreException(ErrorType.CONFLICT, "이미 성공한 결제입니다.");
        }

        return payment;
    }

    @Transactional
    public void updatePaymentStatus(String transactionKey, PaymentStatus status) {
        Payment payment = paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        payment.updateStatus(status);
        payment.updateTransactionKey(transactionKey);
        paymentRepository.savePayment(payment);
    }

    @Transactional
    public void updatePaymentStatusByOrderKey(String orderKey, PaymentStatus status) {
        Payment payment = paymentRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        payment.updateStatus(status);
        paymentRepository.savePayment(payment);
    }
}

