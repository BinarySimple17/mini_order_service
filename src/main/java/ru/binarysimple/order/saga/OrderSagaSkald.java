package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.commands.MakePaymentCommand;
import ru.binarysimple.order.saga.events.OrderCompensateEvent;
import ru.binarysimple.order.dto.commands.ReserveStockCommand;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.events.OrderCreatedEvent;
import ru.binarysimple.order.saga.steps.MakePaymentStep;
import ru.binarysimple.order.saga.steps.ReserveStockStep;
import ru.binarysimple.order.service.NotificationService;

import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderSagaSkald {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final MakePaymentStep makePaymentStep;
    private final ReserveStockStep reserveStockStep;
    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final NotificationService notificationService;

    @KafkaListener(topics = "order.saga.events", groupId = "order-group")
    public void handleOrderCreatedEvent(@Payload OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order {}", event.getOrder().getId());

        OrderSaga saga = getSaga(event.getSagaId(), event.getOrder().getId());
        Order order = getOrder(event.getOrder().getId());

        // Стартуем с первого этапа
        if (saga.getCurrentStep().equals("PENDING")) {
            saga.setCurrentStep("BILLING");
        }

        switch (saga.getCurrentStep()) {
            case "BILLING" -> billingStep(order, saga);
            case "WAREHOUSE" -> warehouseStep(order, saga);
            case "DELIVERY" -> deliveryStep(order, saga);
        }

        notificationService.sendNotification(orderMapper.toOrderResultDto(order));
    }

    @KafkaListener(topics = "order.saga.compensate", groupId = "order-group")
    public void handleOrderCreatedEvent(@Payload OrderCompensateEvent event) {
        log.info("Received OrderCompensateEvent for Order {}", event.getOrder().getId());

        OrderSaga saga = getSaga(event.getSagaId(), event.getOrder().getId());
        Order order = getOrder(event.getOrder().getId());

        switch (saga.getCompensateStep()) {
            case "BILLING" -> billingCompensateStep(order, saga);
            case "WAREHOUSE" -> warehouseCompensateStep(order, saga);
            case "DELIVERY" -> deliveryCompensateStep(order, saga);
        }

        notificationService.sendNotification(orderMapper.toOrderResultDto(order));
    }

    private void billingCompensateStep(Order order, OrderSaga saga) {
// синхронный и последний
            MakePaymentCommand paymentCmd = new MakePaymentCommand(orderMapper.toOrderResultDto(order), saga.getId());
            compensateStep(makePaymentStep, paymentCmd);

            saga.setStatus("COMPENSATED");
            saga.setCompensateStep(null);
            sagaRepository.save(saga);
    }

    private void warehouseCompensateStep(Order order, OrderSaga saga) {

        ReserveStockCommand stockCmd = new ReserveStockCommand(orderMapper.toOrderResultDto(order), saga.getId());

        compensateStep(reserveStockStep, stockCmd);
    }

    private void deliveryCompensateStep(Order order, OrderSaga saga) {

    }

    private void billingStep(Order order, OrderSaga saga) {
        // Первый шаг: оплата
        saga.setStatus("PROCESSING");
        saga.setCurrentStep("BILLING");
        sagaRepository.save(saga);

        MakePaymentCommand paymentCmd = new MakePaymentCommand(orderMapper.toOrderResultDto(order), saga.getId());
        makePaymentStep.execute(paymentCmd);

//        StepExecutionResult<PaymentProcessedEvent> paymentResult = makePaymentStep.execute(paymentCmd);
//
//        if (!paymentResult.isSuccess()) {
//            log.error("Payment step failed for Order {}: {}", order.getId(), paymentResult.getFailureReason());
//            try {
//                order.setStatus(OrderStatus.valueOf(paymentResult.getFailureReason()));
//            } catch (IllegalArgumentException e) {
//                order.setStatus(PAYMENT_FAILED);
//            }
//            orderRepository.save(order);
//
//            saga.setStatus("FAILED");
//            sagaRepository.save(saga);
//            // нечего откатывать, так что не публикуем
//            return;
//        }
//
//        // Оплата успешна
//        order.setStatus(OrderStatus.PENDING_PAYMENT);
//        orderRepository.save(order);
//
//        // Переходим к следующему шагу
//        saga.setCurrentStep("WAREHOUSE");
//        sagaRepository.save(saga);
//
//        // Публикуем событие в Kafka
//        OrderCreatedEvent nextEvent = OrderCreatedEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getName(), saga.getId());
//        kafkaTemplate.send("order.saga.events", "order_paid" + order.getId(), nextEvent);
    }

    private void warehouseStep(Order order, OrderSaga saga) {

        ReserveStockCommand stockCmd = new ReserveStockCommand(orderMapper.toOrderResultDto(order), saga.getId());

        compensateStep(reserveStockStep, stockCmd);
//
//        StepExecutionResult<StockReservedEvent> stockResult;
//
//        if (saga.getStatus().equals("FAILED")) {
//            stockResult = StepExecutionResult.failure("Stock reservation failed");
//        } else {
//            stockResult = reserveStockStep.execute(stockCmd);
//        }
//
//        if (!stockResult.isSuccess()) {
//            log.error("Stock reservation step failed for Order {}: {}", order.getId(), stockResult.getFailureReason());
//            order.setStatus(OrderStatus.RESERVATION_FAILED);
//            orderRepository.save(order);
//
//            saga.setStatus("FAILED");
//            sagaRepository.save(saga);
//
//            // Компенсируем предыдущий шаг (отмена оплаты)
//            MakePaymentCommand paymentCmd = new MakePaymentCommand(order);
//            compensateStep(makePaymentStep, paymentCmd);
//            return;
//        }
//
//        // Резервирование успешно
//        order.setStatus(OrderStatus.PENDING_RESERVATION);
//        orderRepository.save(order);
//
//        // Переходим к следующему шагу
//        saga.setCurrentStep("DELIVERY");
//        sagaRepository.save(saga);

    }

    private void deliveryStep(Order order, OrderSaga saga) {
        // Завершаем Saga
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        saga.setStatus("COMPLETED");
        sagaRepository.save(saga);

        log.info("Saga completed successfully for Order {}", order.getId());
    }

    private OrderSaga getSaga(UUID id, Long orderId) {
        // находим OrderSaga для отслеживания состояния
        OrderSaga saga = sagaRepository.findById(id).orElseGet(() -> {
            log.warn("OrderSaga not found for ID: {}. Initializing new saga state.", id);
            OrderSaga newSaga = new OrderSaga();
            newSaga.setId(id);
            newSaga.setOrderId(orderId);
            newSaga.setCurrentStep("PENDING");
            return newSaga;
        });
        return saga;
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    private <C> void compensateStep(SagaStep<C, ?> step, C command) {
        log.info("Compensating step: {}", step.getClass().getSimpleName());
        StepExecutionResult<?> compensationResult = step.compensate(command);
        if (!compensationResult.isSuccess()) {
            log.error("Compensation failed for step {}: {}", step.getClass().getSimpleName(), compensationResult.getFailureReason());
        } else {
            log.info("Compensation successful for step: {}", step.getClass().getSimpleName());
        }
    }
}