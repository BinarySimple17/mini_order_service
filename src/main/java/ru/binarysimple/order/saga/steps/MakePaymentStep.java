package ru.binarysimple.order.saga.steps;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.commands.MakePaymentCommand;
import ru.binarysimple.order.exception.BillingServiceException;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.model.SagaExpectedEventType;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.SagaStep;
import ru.binarysimple.order.saga.StepExecutionResult;
import ru.binarysimple.order.saga.events.OrderCreatedEvent;
import ru.binarysimple.order.saga.events.PaymentProcessedEvent;

import java.time.LocalDateTime;
import java.util.UUID;

import static ru.binarysimple.order.model.OrderStatus.PAYMENT_FAILED;

@Component
@AllArgsConstructor
@Slf4j
public class MakePaymentStep implements SagaStep<MakePaymentCommand, PaymentProcessedEvent> {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private BillingServiceClient billingServiceClient; // Синхронный вызов
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public StepExecutionResult<PaymentProcessedEvent> execute(MakePaymentCommand command) {
        UUID sagaId = command.getSagaId();
        Long orderId = command.getOrder().getId();

        // 1. Отправить команду в Kafka для оплаты
        kafkaTemplate.send("billing.commands.make-payment", command);

        // 2. Обновить состояние саги в БД: указать, что ждем StockReservedEvent
        OrderSaga saga = sagaRepository.findById(sagaId).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + sagaId));
        saga.setStatus("WAITING"); // Новый статус
        saga.setExpectedEventType(SagaExpectedEventType.PAYMENT_REQUESTED_EVENT); // <-- Используем ENUM
        saga.setExpectedEventOrderId(orderId); // Для быстрого поиска
        saga.setWaitTimeoutAt(LocalDateTime.now().plusSeconds(30)); // Установить таймаут
        sagaRepository.save(saga);

        // 3. Вернуть "ожидающий" результат
        return StepExecutionResult.waiting();
    }

    @KafkaListener(topics = "billing.commands.make-payment", groupId = "order-state-processor-group")
    // Отдельная группа!
    public void handleMakePaymentCommand(@Payload MakePaymentCommand event) {
        log.info("Received MakePaymentCommand for Order {}: status={}", event.getOrder().getId(), event.getOrder().getStatus());

        // 1. Найти сагу в БД по orderId и статусу ожидания
        OrderSaga saga = sagaRepository.findByExpectedEventOrderIdAndExpectedEventTypeAndStatus(
                event.getOrder().getId(),
                SagaExpectedEventType.PAYMENT_REQUESTED_EVENT,
                "WAITING"
        ).orElseGet(() -> {
            log.warn("Received MakePaymentCommand for unexpected/unwaited Order: {}", event.getOrder().getId());
            return null; // Событие не ожидалось, игнорируем
        });

        if (saga == null) {
            return;
        }

        Order order = orderRepository.findById(event.getOrder().getId()).orElseThrow(() -> new RuntimeException("Order not found for ID: " + event.getOrder().getId()));

        // 2. Проверить, не истекло ли время ожидания
        if (LocalDateTime.now().isAfter(saga.getWaitTimeoutAt())) {
            log.warn("Received MakePaymentCommand for Order {}, but timeout expired at {}", event.getOrder().getId(), saga.getWaitTimeoutAt());
            // Обработать таймаут: установить статус FAILED, запустить компенсацию
            handleTimeout(saga, order);
            return;
        }

        //выполняем платеж синхронный
        StepExecutionResult<PaymentProcessedEvent> result = callBillingService(event);

//         3. Проверить результат события
        if (!result.isSuccess()) {
            saga.setStatus("FAILED");
            sagaRepository.save(saga);
            sagaRepository.save(saga);
            try {
                order.setStatus(OrderStatus.valueOf(result.getFailureReason()));
            } catch (IllegalArgumentException e) {
                order.setStatus(PAYMENT_FAILED);
            }
            orderRepository.save(order);
            // нечего откатывать, так что не публикуем
        } else {
            // 4. Обновить состояние саги: сбросить ожидание, перейти к следующему шагу
            saga.setStatus("PROCESSING"); // Снова активна
            saga.setCurrentStep("WAREHOUSE"); // Следующий шаг
            saga.setExpectedEventType(null); // Сброс ожидания
            saga.setExpectedEventOrderId(null);
            saga.setWaitTimeoutAt(null);
            sagaRepository.save(saga);

            // 5. Повторно запустить оркестратор
            OrderCreatedEvent nextEvent = OrderCreatedEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getSimpleName(), saga.getId());
            kafkaTemplate.send("order.saga.events", "order_payment_made_" + event.getOrder().getId(), nextEvent);

        }
    }

    private void handleTimeout(OrderSaga saga, Order order) {
        log.error("Payment failed timeout for Order {} in Saga {}", saga.getOrderId(), saga.getId());

        saga.setStatus("FAILED");
        sagaRepository.save(saga);
        order.setStatus(PAYMENT_FAILED);
        orderRepository.save(order);
        // нечего откатывать, так что не публикуем
    }


    //    @Override
    private StepExecutionResult<PaymentProcessedEvent> callBillingService(MakePaymentCommand command) {
        OperationDto response = null;
        try {
            // Вызов Billing Service
            // успешно, если вызов не бросил исключение
            response = billingServiceClient.makePayment(command.getOrder());
        } catch (BillingServiceException billingServiceException) {
            return StepExecutionResult.failure(OrderStatus.INSUFFICIENT_FUNDS.name());
        } catch (Exception e) {
            return StepExecutionResult.failure("Exception during payment: " + e.getMessage());
        }
        return StepExecutionResult.success(new PaymentProcessedEvent(response));
    }

    @Override
    public StepExecutionResult<PaymentProcessedEvent> compensate(MakePaymentCommand command) {
        OperationDto response = null;
        try {
            // Отмена резерва
//            CancelPaymentCommand cancelCmd = new CancelPaymentCommand(command.getOrder());
            response = billingServiceClient.cancelPayment(command.getOrder()); // Вызов метода отмены
            //отмена успешна, если вызов не бросил исключение
            return StepExecutionResult.success(new PaymentProcessedEvent(response)); // Или вернуть событие типа CancelledPaymentEvent
        } catch (Exception e) {
            return StepExecutionResult.failure("Exception during payment cancellation: " + e.getMessage());
        }
    }
}