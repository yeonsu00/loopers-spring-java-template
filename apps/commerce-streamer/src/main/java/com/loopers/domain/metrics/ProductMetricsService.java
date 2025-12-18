package com.loopers.domain.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProductMetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    @Transactional
    public void incrementLikeCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.create(productId);
                    productMetricsRepository.saveProductMetrics(newMetrics);
                    return newMetrics;
                });
        metrics.incrementLikeCount();
        productMetricsRepository.saveProductMetrics(metrics);
    }

    @Transactional
    public void decrementLikeCount(Long productId) {
        productMetricsRepository.findByProductId(productId)
                .ifPresent(metrics -> {
                    metrics.decrementLikeCount();
                    productMetricsRepository.saveProductMetrics(metrics);
                });
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.create(productId);
                    productMetricsRepository.saveProductMetrics(newMetrics);
                    return newMetrics;
                });
        metrics.incrementViewCount();
        productMetricsRepository.saveProductMetrics(metrics);
    }

    @Transactional
    public void incrementSalesCount(Long productId, Integer quantity) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                .orElseGet(() -> {
                    ProductMetrics newMetrics = ProductMetrics.create(productId);
                    productMetricsRepository.saveProductMetrics(newMetrics);
                    return newMetrics;
                });
        metrics.incrementSalesCount(quantity);
        productMetricsRepository.saveProductMetrics(metrics);
    }
}

