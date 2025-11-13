package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public Order createOrder(Long userId, Delivery delivery) {
        return Order.createOrder(userId, delivery);
    }

    public void createOrderItem(Order order, Product product, Integer quantity) {
        OrderItem orderItem = OrderItem.createOrderItem(
                order,
                product.getId(),
                product.getName(),
                product.getPrice().getPrice(),
                quantity
        );

        order.addOrderItem(orderItem);
    }

    public void addTotalPrice(Order order, int price, int quantity) {
        order.addPrice(price * quantity);
    }
}
