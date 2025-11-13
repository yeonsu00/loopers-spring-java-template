package com.loopers.domain.product;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public void reduceStock(Long productId, Integer quantity) {
    }

    public void validateStock(Long aLong, Integer quantity) {
    }

    public Optional<Product> findProductById(Long aLong) {
    }
}
