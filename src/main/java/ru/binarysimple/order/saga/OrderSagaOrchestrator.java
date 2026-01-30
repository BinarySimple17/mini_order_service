package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.dto.commands.MakePaymentCommand;
import ru.binarysimple.order.dto.commands.PaymentProcessedEvent;
import ru.binarysimple.order.dto.commands.ReserveStockCommand;
import ru.binarysimple.order.dto.commands.StockReservedEvent;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.events.OrderCreatedEvent;
import ru.binarysimple.order.saga.events.OrderPaidEvent;
import ru.binarysimple.order.saga.events.OrderStockReservedEvent;
import ru.binarysimple.order.saga.events.OrderStockReservedFailedEvent;
import ru.binarysimple.order.saga.steps.MakePaymentStep;
import ru.binarysimple.order.saga.steps.ReserveStockStep;

import java.util.UUID;

import static ru.binarysimple.order.model.OrderStatus.PAYMENT_FAILED;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final MakePaymentStep makePaymentStep;
    private final ReserveStockStep reserveStockStep;
    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.saga.events", groupId = "order-service-group")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order {}", event.getOrder().getId());

        OrderSaga saga = getSaga(event.getSagaId(), event.getOrder().getId());

        saga.setCurrentStep("BILLING");
        saga.setStatus("PROCESSING");
        sagaRepository.save(saga);

        Order order = getOrder(event.getOrder().getId());

        // Первый шаг: оплата
        MakePaymentCommand paymentCmd = new MakePaymentCommand(order);
        StepExecutionResult<PaymentProcessedEvent> paymentResult = makePaymentStep.execute(paymentCmd);

        if (!paymentResult.isSuccess()) {
            log.error("Payment step failed for Order {}: {}", order.getId(), paymentResult.getFailureReason());
            try {
                order.setStatus(OrderStatus.valueOf(paymentResult.getFailureReason()));
            } catch (IllegalArgumentException e) {
                order.setStatus(PAYMENT_FAILED);
            }
            orderRepository.save(order);

            saga.setStatus("FAILED");
            sagaRepository.save(saga);
            return;
        }

        // Оплата успешна
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);

        // Публикуем событие в Kafka
        OrderCreatedEvent nextEvent = new OrderCreatedEvent(orderMapper.toOrderResultDto(order), this.getClass().getName(), saga.getId());
        kafkaTemplate.send("order.saga.events", "order_paid" + order.getId(), nextEvent);
    }

    @KafkaListener(topics = "order.saga.events", groupId = "order-service-group")
    public void handleOrderPaidEvent(OrderPaidEvent event) {

        OrderSaga saga = getSaga(event.getSagaId(), event.getOrder().getId());

        Order order = getOrder(event.getOrder().getId());

        // Переходим к следующему шагу
        saga.setCurrentStep("WAREHOUSE");
        sagaRepository.save(saga);

        // Второй шаг: резервирование товаров
        ReserveStockCommand stockCmd = new ReserveStockCommand(orderMapper.toOrderResultDto(order));
        StepExecutionResult<StockReservedEvent> stockResult = reserveStockStep.execute(stockCmd);

        if (!stockResult.isSuccess()) {
            log.error("Stock reservation step failed for Order {}: {}", order.getId(), stockResult.getFailureReason());
            order.setStatus(OrderStatus.RESERVATION_FAILED);
            orderRepository.save(order);

            saga.setStatus("COMPENSATING");
            sagaRepository.save(saga);

            // Компенсируем предыдущий шаг (отмена оплаты)
            // Публикуем событие в Kafka
            OrderStockReservedFailedEvent nextEvent = new OrderStockReservedFailedEvent(orderMapper.toOrderResultDto(order), this.getClass().getName(), saga.getId());
            kafkaTemplate.send("order.saga.events", "order_stock_failed" + order.getId(), nextEvent);
            return;
        }

        // Резервирование успешно
        order.setStatus(OrderStatus.PENDING_RESERVATION);
        orderRepository.save(order);

        // Публикуем событие в Kafka
        OrderStockReservedEvent nextEvent = new OrderStockReservedEvent(orderMapper.toOrderResultDto(order), this.getClass().getName(), saga.getId());
        kafkaTemplate.send("order.saga.events", "order_stock_reserved" + order.getId(), nextEvent);

    }

    @KafkaListener(topics = "order.saga.events", groupId = "order-service-group")
    public void handleOrderStockReservedFailedEvent(OrderStockReservedFailedEvent event) {

        OrderSaga saga = getSaga(event.getSagaId(), event.getOrder().getId());

        Order order = getOrder(event.getOrder().getId());

        // Компенсируем оплату
        MakePaymentCommand paymentCmd = new MakePaymentCommand(order);
        compensateStep(makePaymentStep, paymentCmd);
//        StepExecutionResult<PaymentProcessedEvent> paymentResult = makePaymentStep.compensate(paymentCmd);

        // Завершаем Saga
//        order.setStatus(PAYMENT_FAILED);
//        orderRepository.save(order);

        saga.setStatus("FAILED");
        sagaRepository.save(saga);

        log.info("Saga completed bad scenario for Order {}", order.getId());
    }

    @KafkaListener(topics = "order.saga.events", groupId = "order-service-group")
    public void handleOrderStockReservedEvent(OrderStockReservedEvent event) {

        OrderSaga saga = getSaga(event.getSagaId(), event.getOrder().getId());

        Order order = getOrder(event.getOrder().getId());

        // Переходим к следующему шагу
        saga.setCurrentStep("DELIVERY");
        sagaRepository.save(saga);

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
        if (compensationResult.isSuccess()) {
            log.info("Compensation successful for step: {}", step.getClass().getSimpleName());
        } else {
            log.error("Compensation failed for step {}: {}", step.getClass().getSimpleName(), compensationResult.getFailureReason());
        }
    }
}
