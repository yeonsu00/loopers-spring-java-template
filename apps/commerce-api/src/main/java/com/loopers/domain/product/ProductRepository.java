package com.loopers.domain.product;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository {

    Optional<Product> findById(Long productId);

}
