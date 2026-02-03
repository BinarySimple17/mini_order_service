package ru.binarysimple.order.saga.steps;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.commands.MakePaymentCommand;
import ru.binarysimple.order.exception.BillingServiceException;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.model.saga.OrderSagaStatus;
import ru.binarysimple.order.model.saga.SagaExpectedEventType;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.SagaStep;
import ru.binarysimple.order.saga.StepExecutionResult;
import ru.binarysimple.order.saga.events.OrderCreatedEvent;
import ru.binarysimple.order.saga.events.PaymentProcessedEvent;

import java.time.LocalDateTime;
import java.util.UUID;

import static ru.binarysimple.order.model.saga.OrderSagaStatus.PROCESSING;
import static ru.binarysimple.order.model.saga.OrderSagaStatus.WAITING;
import static ru.binarysimple.order.model.saga.OrderSagaStep.WAREHOUSE;
import static ru.binarysimple.order.model.OrderStatus.*;

@Component
@AllArgsConstructor
@Slf4j
@Transactional
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

        // 1. Сохранить состояние саги в БД: указать, что ожидаем ответ
        OrderSaga saga = sagaRepository.findById(sagaId).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + sagaId));
        saga.setStatus(WAITING);
        saga.setExpectedEventType(SagaExpectedEventType.PAYMENT_REQUESTED_EVENT);
        saga.setExpectedEventOrderId(orderId);
        saga.setWaitTimeoutAt(LocalDateTime.now().plusSeconds(30));
        sagaRepository.save(saga);

        return processPayment(command);
    }

    // Метод для синхронного выполнения оплаты и отправки результата
    public StepExecutionResult<PaymentProcessedEvent> processPayment(MakePaymentCommand command) {
//    public StepExecutionResult<PaymentProcessedEvent> processPayment(MakePaymentCommand command) {
        UUID sagaId = command.getSagaId();
        Long orderId = command.getOrder().getId();

        log.info("Processing payment for Order {} in Saga {}", orderId, sagaId);

        // 1. Найти сагу в БД по orderId и статусу ожидания
        OrderSaga saga = sagaRepository.findById(sagaId).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + sagaId));

//        // Проверяем, что сага действительно ожидает событие оплаты
        // не проверяем, потому что синхронно
//        if (!"WAITING".equals(saga.getStatus()) ||
//                !SagaExpectedEventType.PAYMENT_REQUESTED_EVENT.equals(saga.getExpectedEventType()) ||
//                !orderId.equals(saga.getExpectedEventOrderId())) {
//            log.warn("Saga {} is not in expected state for payment processing. Status: {}, ExpectedEventType: {}, ExpectedOrderId: {}",
//                    sagaId, saga.getStatus(), saga.getExpectedEventType(), saga.getExpectedEventOrderId());
//            return StepExecutionResult.failure("Saga not in expected state");
//        }

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found for ID: " + orderId));

        // 2. Проверить, не истекло ли время ожидания
        if (LocalDateTime.now().isAfter(saga.getWaitTimeoutAt())) {
            log.warn("Payment processing for Order {} in Saga {} failed due to timeout", orderId, sagaId);
            // Обработать таймаут
            handleTimeout(saga, order);
            return StepExecutionResult.failure("Payment timeout");
        }

        // 3. Выполняем синхронный вызов сервиса оплаты
        StepExecutionResult<PaymentProcessedEvent> result = callBillingService(command);

        // 4. Проверить результат вызова
        if (!result.isSuccess()) {
            log.error("Billing service call failed for Order {}: {}", order.getId(), result.getFailureReason());
            saga.setStatus(OrderSagaStatus.FAILED);
            sagaRepository.save(saga);
            try {
                order.setStatus(OrderStatus.valueOf(result.getFailureReason()));
            } catch (IllegalArgumentException e) {
                order.setStatus(PAYMENT_FAILED);
            }
            orderRepository.save(order);
            // Сохраняем состояние, но не публикуем события, так как отката нет
            return result;
        }

        // 5. Успешная оплата: сбросить ожидание и обновить статус саги
        saga.setStatus(PROCESSING);
        saga.setCurrentStep(WAREHOUSE);
        saga.setExpectedEventType(null);
        saga.setExpectedEventOrderId(null);
        saga.setWaitTimeoutAt(null);
        sagaRepository.save(saga);

        order.setStatus(PENDING_PAYMENT);
        orderRepository.save(order);

        // 6. Отправить событие в Kafka для перехода на следующий шаг
        OrderCreatedEvent nextEvent = OrderCreatedEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getSimpleName(), saga.getId());
        kafkaTemplate.send("order.saga.events", command.getOrder().getId().toString(), nextEvent);

        log.info("Payment processed and event sent for Order {}", order.getId());
        return result;
    }

    private void handleTimeout(OrderSaga saga, Order order) {
        log.error("Payment failed timeout for Order {} in Saga {}", saga.getOrderId(), saga.getId());

        saga.setStatus(OrderSagaStatus.FAILED);
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

    @Override
    public StepExecutionResult<PaymentProcessedEvent> processEvent(PaymentProcessedEvent event) {
        log.info("MakePaymentStep was synchronously made payment when execute()");
        return StepExecutionResult.waiting();
    }
}