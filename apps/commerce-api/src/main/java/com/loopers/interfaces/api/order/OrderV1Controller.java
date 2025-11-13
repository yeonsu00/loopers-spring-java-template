package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderV1Dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping
    @Override
    public ApiResponse<OrderResponse> order(
            @RequestHeader("X-USER-ID") String loginId,
            @Valid @RequestBody OrderV1Dto.OrderRequest orderRequest
    ) {
        OrderCommand.CreateOrderCommand createOrderCommand = orderRequest.toCommand(loginId);
        OrderInfo orderInfo = orderFacade.createOrder(createOrderCommand);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);

        return ApiResponse.success(response);
    }

}
