package ru.binarysimple.order.saga;

import org.springframework.stereotype.Component;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;

@Component
public interface OrderSagaManager {
    OrderResultDto createOrder(OrderDto orderDto);
    void execute();
    void addStep(SagaStep step);
    void cancelOrder(Long orderId);
}
