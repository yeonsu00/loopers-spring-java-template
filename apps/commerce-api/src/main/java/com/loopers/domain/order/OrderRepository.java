package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository {
    
    List<Order> findOrdersByUserId(Long userId);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
}
