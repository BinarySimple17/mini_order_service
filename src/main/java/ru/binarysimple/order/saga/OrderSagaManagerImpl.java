package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderSagaManagerImpl implements OrderSagaManager {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final BillingServiceClient billingServiceClient;

    @Override
    @Transactional
    public OrderResultDto createOrder(OrderDto orderDto) {
        Order order = orderMapper.toEntity(orderDto);
        order.setStatus(OrderStatus.NEW);

        // Сохраняем заказ в статусе NEW
        Order savedOrder = orderRepository.save(order);

        try {
            // Шаг 1: Резервирование средств
            billingServiceClient.reserveFunds(savedOrder);
            savedOrder.setStatus(OrderStatus.RESERVING_PAYMENT);
            savedOrder = orderRepository.save(savedOrder);

            // Шаг 2: Подтверждение платежа
            billingServiceClient.confirmPayment(savedOrder);
            savedOrder.setStatus(OrderStatus.PAID);
            savedOrder = orderRepository.save(savedOrder);

            // Шаг 3: Создание задания на доставку
            // deliveryServiceClient.createDelivery(savedOrder);
            // savedOrder.setStatus(OrderStatus.IN_PROGRESS);
            // savedOrder = orderRepository.save(savedOrder);

            log.info("Order saga completed successfully for order {}", savedOrder.getId());
            return orderMapper.toOrderResultDto(savedOrder);

        } catch (Exception e) {
            log.error("Order saga failed for order {}: {}", savedOrder.getId(), e.getMessage(), e);
            // Запускаем компенсирующие действия
            compensateOrderSaga(savedOrder.getId());
            throw e;
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.RESERVING_PAYMENT) {
            try {
                // Компенсирующее действие: возврат средств
                billingServiceClient.refundPayment(order);
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
                log.info("Order canceled and payment refunded for order {}", orderId);
            } catch (Exception e) {
                log.error("Failed to cancel order {}: {}", orderId, e.getMessage(), e);
                // Можно добавить повторные попытки или отправить в очередь для повторной обработки
                throw new RuntimeException("Failed to cancel order: " + e.getMessage(), e);
            }
        } else {
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
            log.info("Order canceled without payment actions for order {}", orderId);
        }
    }

    @Transactional
    protected void compensateOrderSaga(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        log.info("Starting compensation for order {} with status {}", orderId, order.getStatus());

        try {
            // В зависимости от текущего состояния выполняем соответствующие компенсирующие действия
            switch (order.getStatus()) {
                case RESERVING_PAYMENT -> {
                    // Отмена резервирования средств
                    billingServiceClient.cancelReservation(order);
                    order.setStatus(OrderStatus.CANCELED);
                    orderRepository.save(order);
                    log.info("Compensation: Reservation canceled for order {}", orderId);
                }
                case PAID -> {
                    // Возврат средств
                    billingServiceClient.refundPayment(order);
                    order.setStatus(OrderStatus.CANCELED);
                    orderRepository.save(order);
                    log.info("Compensation: Payment refunded for order {}", orderId);
                }
                case NEW -> {
                    // Просто отменяем заказ
                    order.setStatus(OrderStatus.CANCELED);
                    orderRepository.save(order);
                    log.info("Compensation: Order canceled for order {}", orderId);
                }
                default -> log.info("No compensation needed for order {} with status {}", orderId, order.getStatus());
            }
        } catch (Exception e) {
            log.error("Failed to compensate order {}: {}", orderId, e.getMessage(), e);
            // Можно добавить повторные попытки или отправить в очередь для повторной обработки
            throw new RuntimeException("Failed to compensate order: " + e.getMessage(), e);
        }
    }
}
