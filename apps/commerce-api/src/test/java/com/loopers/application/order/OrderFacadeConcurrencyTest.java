package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.application.user.UserCommand;
import com.loopers.domain.product.LikeCount;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.Stock;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class OrderFacadeConcurrencyTest {

    @Autowired
    private OrderLockFacade orderLockFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointFacade pointFacade;

    @MockitoSpyBean
    private com.loopers.domain.brand.BrandService brandService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private String loginId;
    private List<Long> productIds;
    private int initialPoint;

    @BeforeEach
    void setUp() {
        loginId = "testUser";
        UserCommand.SignupCommand signupCommand = new UserCommand.SignupCommand(
                loginId,
                "test@test.com",
                "2000-01-01",
                "M"
        );
        userService.signup(signupCommand);

        initialPoint = 1000000;
        PointCommand.ChargeCommand chargeCommand = new PointCommand.ChargeCommand(loginId, initialPoint);
        pointFacade.chargePoint(chargeCommand);

        productIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Product product = Product.createProduct(
                    "상품" + i,
                    1L,
                    Price.createPrice(1000 * i),
                    LikeCount.createLikeCount(0),
                    Stock.createStock(100)
            );
            productRepository.saveProduct(product);
            productIds.add(product.getId());
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행할 경우, 포인트가 정상적으로 차감되어야 한다.")
    @Test
    void shouldDeductPointCorrectly_whenSameUserCreatesMultipleOrdersConcurrently() throws Exception {
        // arrange
        int orderCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);
        List<Future<OrderInfo>> futures = new ArrayList<>();

        int[] quantities = {1, 2, 3, 4, 5};
        int expectedTotalDeduction = 0;

        // act
        for (int i = 0; i < orderCount; i++) {
            final int index = i;
            final int quantity = quantities[i];
            final Long productId = productIds.get(index);
            expectedTotalDeduction += (1000 * (index + 1)) * quantity;

            Future<OrderInfo> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    OrderCommand.DeliveryCommand deliveryCommand = new OrderCommand.DeliveryCommand(
                            "홍길동",
                            "010-1234-5678",
                            "서울시 강남구",
                            "테헤란로 " + (index + 1)
                    );

                    OrderCommand.OrderItemCommand orderItemCommand = new OrderCommand.OrderItemCommand(
                            productId,
                            quantity
                    );

                    OrderCommand.CreateOrderCommand createOrderCommand = new OrderCommand.CreateOrderCommand(
                            loginId,
                            List.of(orderItemCommand),
                            deliveryCommand
                    );

                    return orderLockFacade.createOrder(createOrderCommand);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        List<OrderInfo> results = new ArrayList<>();
        for (Future<OrderInfo> future : futures) {
            results.add(future.get());
        }

        executorService.shutdown();

        // assert
        assertThat(results).hasSize(orderCount);

        PointInfo finalPointInfo = pointFacade.getPointInfo(loginId);
        int expectedFinalPoint = initialPoint - expectedTotalDeduction;
        assertThat(finalPointInfo.totalPoint()).isEqualTo(expectedFinalPoint);

        for (int i = 0; i < orderCount; i++) {
            OrderInfo orderInfo = results.get(i);
            int expectedOrderPrice = (1000 * (i + 1)) * quantities[i];
            int actualOrderPrice = orderInfo.orderItems().stream()
                    .mapToInt(item -> item.price() * item.quantity())
                    .sum();
            assertThat(actualOrderPrice).isEqualTo(expectedOrderPrice);
        }
    }

    @DisplayName("동일한 유저가 동일한 상품을 포함한 여러 주문을 동시에 수행할 경우, 포인트가 정상적으로 차감되어야 한다.")
    @Test
    void shouldDeductPointCorrectly_whenSameUserCreatesOrdersWithSameProductConcurrently() throws Exception {
        // arrange
        int orderCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);
        List<Future<OrderInfo>> futures = new ArrayList<>();

        // 모든 주문이 동일한 상품을 사용하지만 수량이 다름
        Long sameProductId = productIds.get(0);
        int productPrice = 1000; // 첫 번째 상품 가격
        int[] quantities = {1, 2, 3, 4, 5};
        int expectedTotalDeduction = 0;
        for (int quantity : quantities) {
            expectedTotalDeduction += productPrice * quantity;
        }

        // act
        for (int i = 0; i < orderCount; i++) {
            final int index = i;
            final int quantity = quantities[i];

            Future<OrderInfo> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    OrderCommand.DeliveryCommand deliveryCommand = new OrderCommand.DeliveryCommand(
                            "홍길동",
                            "010-1234-5678",
                            "서울시 강남구",
                            "테헤란로 " + (index + 1)
                    );

                    OrderCommand.OrderItemCommand orderItemCommand = new OrderCommand.OrderItemCommand(
                            sameProductId,
                            quantity
                    );

                    OrderCommand.CreateOrderCommand createOrderCommand = new OrderCommand.CreateOrderCommand(
                            loginId,
                            List.of(orderItemCommand),
                            deliveryCommand
                    );

                    return orderLockFacade.createOrder(createOrderCommand);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        List<OrderInfo> results = new ArrayList<>();
        for (Future<OrderInfo> future : futures) {
            results.add(future.get());
        }

        executorService.shutdown();

        // assert
        assertThat(results).hasSize(orderCount);

        // assert
        PointInfo finalPointInfo = pointFacade.getPointInfo(loginId);
        int expectedFinalPoint = initialPoint - expectedTotalDeduction;
        assertThat(finalPointInfo.totalPoint()).isEqualTo(expectedFinalPoint);

        // assert
        for (int i = 0; i < orderCount; i++) {
            OrderInfo orderInfo = results.get(i);
            int expectedOrderPrice = productPrice * quantities[i];
            int actualOrderPrice = orderInfo.orderItems().stream()
                    .mapToInt(item -> item.price() * item.quantity())
                    .sum();
            assertThat(actualOrderPrice).isEqualTo(expectedOrderPrice);
        }
    }
}

