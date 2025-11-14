package com.loopers.domain.user;

import com.loopers.domain.order.Order;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<User> findByLoginId(String loginId);

    List<Order> findOrdersByUserId(Long userId);
}
