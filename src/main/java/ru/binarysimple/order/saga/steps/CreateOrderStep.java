package ru.binarysimple.order.saga.steps;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.SagaStep;

@RequiredArgsConstructor
@Slf4j
public class CreateOrderStep implements SagaStep {

    private final OrderDto orderDto;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    @Getter
    private Order savedOrder;

    @Override
    public void perform() throws Exception {
        log.info("Creating order...");
        
        Order order = orderMapper.toEntity(orderDto);
        order.setStatus(OrderStatus.NEW);
        
        // Устанавливаем связь между заказом и позициями
        order.getOrderPositions().forEach(position -> position.setOrder(order));
        
        // Сохраняем заказ в статусе NEW
        savedOrder = orderRepository.save(order);
        
        log.info("Order created successfully with ID: {}", savedOrder.getId());
    }

    @Override
    public void compensate() throws Exception {
        if (savedOrder != null) {
            log.info("Compensating order creation for order ID: {}", savedOrder.getId());
            
            // Вместо удаления заказа, устанавливаем статус CANCELED
            savedOrder.setStatus(OrderStatus.CANCELED);
            orderRepository.save(savedOrder);
            
            log.info("Order creation compensated for order ID: {}", savedOrder.getId());
        } else {
            log.warn("Cannot compensate order creation: savedOrder is null");
        }
    }
}