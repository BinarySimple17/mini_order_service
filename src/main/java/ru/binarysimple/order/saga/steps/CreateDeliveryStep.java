package ru.binarysimple.order.saga.steps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.binarysimple.order.client.DeliveryServiceClient;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.SagaStep;

@RequiredArgsConstructor
@Slf4j
public class CreateDeliveryStep implements SagaStep {

    private final Order order;
    private final DeliveryServiceClient deliveryServiceClient;
    private final OrderRepository orderRepository;
    private final boolean deliveryCreated = false;

    @Override
    public void perform() throws Exception {
        log.info("Not implemented yet: perform delivery");
//        log.info("Creating delivery for order {}...", order.getId());
//
//        // Создание задания на доставку
//         deliveryServiceClient.createDelivery(order);
//
//        // Обновляем статус заказа
//        order.setStatus(OrderStatus.IN_PROGRESS);
//        orderRepository.save(order);
//
//        deliveryCreated = true;
//        log.info("Delivery created successfully for order {}", order.getId());
    }

    @Override
    public void compensate() throws Exception {
        log.info("Not implemented yet: compensate delivery");
//        if (deliveryCreated) {
//            log.info("Canceling delivery for order {}", order.getId());
//
//            // Отмена задания на доставку
//            // deliveryServiceClient.cancelDelivery(order);
//
//            // Обновляем статус заказа
//            order.setStatus(OrderStatus.CANCELED);
//            orderRepository.save(order);
//
//            log.info("Delivery canceled for order {}", order.getId());
//        } else {
//            log.warn("Cannot compensate delivery creation: delivery was not created for order {}", order.getId());
//        }
    }
}