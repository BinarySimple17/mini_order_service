package ru.binarysimple.order.saga.steps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.SagaStep;

@RequiredArgsConstructor
@Slf4j
public class ConfirmPaymentStep implements SagaStep {

    private final Order order;
    private final BillingServiceClient billingServiceClient;
    private final OrderRepository orderRepository;
    private final boolean paymentConfirmed = false;

    @Override
    public void perform() throws Exception {
        log.info("Not implemented yet");
//        log.info("Confirming payment for order {}...", order.getId());
//
////        // Подтверждение платежа
//        billingServiceClient.confirmPayment(order);
//
//        // Обновляем статус заказа
//        order.setStatus(OrderStatus.PAID);
//        orderRepository.save(order);
//
//        paymentConfirmed = true;
//        log.info("Payment confirmed successfully for order {}", order.getId());
    }

    @Override
    public void compensate() throws Exception {
        log.info("Not implemented yet");
//        if (paymentConfirmed) {
//            log.info("Refunding payment for order {}", order.getId());
//
//            // Возврат средств
//            billingServiceClient.refundPayment(order);
//
//            // Обновляем статус заказа
//            order.setStatus(OrderStatus.CANCELED);
//            orderRepository.save(order);
//
//            log.info("Payment refunded for order {}", order.getId());
//        } else {
//            log.warn("Cannot compensate payment confirmation: payment was not confirmed for order {}", order.getId());
//        }
    }
}