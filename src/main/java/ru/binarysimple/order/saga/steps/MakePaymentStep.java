package ru.binarysimple.order.saga.steps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.SagaStep;

@RequiredArgsConstructor
@Slf4j
public class MakePaymentStep implements SagaStep {

    private final Order order;
    private final BillingServiceClient billingServiceClient;
    private final OrderRepository orderRepository;
    private boolean paymentSuccessful = false;

    @Override
    public void perform() throws Exception {
        log.info("Making payment for order {}...", order.getId());
        
        // Оплата заказа
        billingServiceClient.makePayment(order);
        
        // Обновляем статус заказа
        order.setStatus(OrderStatus.PAID);

        orderRepository.save(order);
        
        paymentSuccessful = true;
        log.info("Paid successfully for order {}", order.getId());
    }

    @Override
    public void compensate() throws Exception {
        if (paymentSuccessful) {
            log.info("Canceling payment for order {}", order.getId());
            
            // Отмена резервирования средств
             billingServiceClient.cancelPayment(order);
            
            // Обновляем статус заказа
            // статус заказа откатится в компенсации создания заказа
//            order.setStatus(OrderStatus.CANCELED);
//            orderRepository.save(order);
            
            log.info("Payment canceled for order {}", order.getId());
        } else {
            log.warn("Cannot compensate payment: payment was not successful for order {}", order.getId());
        }
    }
}