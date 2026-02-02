package ru.binarysimple.order.saga.steps;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.dto.commands.CancelStockReservationCommand;
import ru.binarysimple.order.dto.commands.ReserveStockCommand;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.SagaExpectedEventType;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.SagaStep;
import ru.binarysimple.order.saga.StepExecutionResult;
import ru.binarysimple.order.saga.events.OrderCompensateEvent;
import ru.binarysimple.order.saga.events.OrderCreatedEvent;
import ru.binarysimple.order.saga.events.StockReservedEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
@Slf4j
public class ReserveStockStep implements SagaStep<ReserveStockCommand, StockReservedEvent> {

//    private final Map<Long, CompletableFuture<StockReservedEvent>> pendingRequests = new ConcurrentHashMap<>();
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    public StepExecutionResult<StockReservedEvent> execute(ReserveStockCommand command) {
        UUID sagaId = command.getSagaId();
        Long orderId = command.getOrder().getId();

        // 1. Отправить команду в Kafka для резервирования
        kafkaTemplate.send("warehouse.commands.reserve", command);

        // 2. Обновить состояние саги в БД: указать, что ждем StockReservedEvent
        OrderSaga saga = sagaRepository.findById(sagaId).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + sagaId));
        saga.setStatus("WAITING"); // Новый статус
        saga.setExpectedEventType(SagaExpectedEventType.STOCK_RESERVED_EVENT); // <-- Используем ENUM
        saga.setExpectedEventOrderId(orderId); // Для быстрого поиска
        saga.setWaitTimeoutAt(LocalDateTime.now().plusSeconds(30)); // Установить таймаут
        sagaRepository.save(saga);

        // 3. Вернуть "ожидающий" результат
        return StepExecutionResult.waiting();
    }

    @KafkaListener(topics = "warehouse.responses", groupId = "order-state-processor-group") // Отдельная группа!
    public void handleStockReservationResponse(@Payload StockReservedEvent event) {
        log.info("Received StockReservedEvent for Order {}: status={}", event.getOrderId(), event.getStatus());

        // 1. Найти сагу в БД по orderId и статусу ожидания
        OrderSaga saga = sagaRepository.findByExpectedEventOrderIdAndExpectedEventTypeAndStatus(
                event.getOrderId(),
                SagaExpectedEventType.STOCK_RESERVED_EVENT,
                "WAITING"
        ).orElseGet(() -> {
            log.warn("Received StockReservedEvent for unexpected/unwaited Order: {}", event.getOrderId());
            return null; // Событие не ожидалось, игнорируем
        });

        if (saga == null) {
            return;
        }

        // 2. Проверить, не истекло ли время ожидания
        if (LocalDateTime.now().isAfter(saga.getWaitTimeoutAt())) {
            log.warn("Received StockReservedEvent for Order {}, but timeout expired at {}", event.getOrderId(), saga.getWaitTimeoutAt());
            // Обработать таймаут: установить статус FAILED, запустить компенсацию
            saga.setCompensateStep("BILLING");
            handleTimeout(saga);
            return;
        }

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found for ID: " + event.getOrderId()));

        // 3. Проверить результат события
        if ("RESERVED".equals(event.getStatus())) {
            // 4. Обновить состояние саги: сбросить ожидание, перейти к следующему шагу
            saga.setStatus("PROCESSING"); // Снова активна
            saga.setCurrentStep("DELIVERY"); // Следующий шаг
            saga.setExpectedEventType(null); // Сброс ожидания
            saga.setExpectedEventOrderId(null);
            saga.setWaitTimeoutAt(null);
            sagaRepository.save(saga);

            // 5. Повторно запустить оркестратор
            OrderCreatedEvent nextEvent = OrderCreatedEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getSimpleName(), saga.getId());
            kafkaTemplate.send("order.saga.events", "order_warehouse_reserved_" + event.getOrderId(), nextEvent);

        } else { // "FAILED" или другой статус ошибки
            log.error("Stock reservation failed for Order {} in Saga {}", event.getOrderId(), saga.getId());
            saga.setStatus("COMPENSATING"); // Начать компенсацию
            saga.setCompensateStep("BILLING");
            saga.setExpectedEventType(null);
            saga.setExpectedEventOrderId(null);
            saga.setWaitTimeoutAt(null);
            sagaRepository.save(saga);

            // 5. Повторно запустить оркестратор
            OrderCompensateEvent nextEvent = OrderCompensateEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getSimpleName(), saga.getId());
            kafkaTemplate.send("order.saga.compensate", "order_warehouse_failed_" + event.getOrderId(), nextEvent);
        }
    }

    private void handleTimeout(OrderSaga saga) {
        log.error("Stock reservation failed timeout for Order {} in Saga {}", saga.getOrderId(), saga.getId());

        Order order = orderRepository.findById(saga.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found for ID: " + saga.getOrderId()));

        saga.setStatus("COMPENSATING"); // Начать компенсацию
        saga.setExpectedEventType(null);
        saga.setExpectedEventOrderId(null);
        saga.setWaitTimeoutAt(null);
        sagaRepository.save(saga);

        // 5. Повторно запустить оркестратор
        OrderCompensateEvent nextEvent = OrderCompensateEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getSimpleName(), saga.getId());
        kafkaTemplate.send("order.saga.compensate", "order_warehouse_failed_" + saga.getOrderId(), nextEvent);
    }

    @Override
    public StepExecutionResult<StockReservedEvent> compensate(ReserveStockCommand command) {
        // Отправка команды отмены в Kafka
        kafkaTemplate.send("warehouse.commands.compensate", new CancelStockReservationCommand(command.getOrder().getId(),
                "CANCEL_RESERVATION"));
        // Компенсация асинхронна, не понтно сразу успех/неуспех.
        return StepExecutionResult.waiting(); // Условный успех

//        UUID sagaId = command.getSagaId();
//        Long orderId = command.getOrder().getId();
//
//        // 1. Отправить команду в Kafka для отмены оплаты
//        kafkaTemplate.send("billing.commands.compensate", command);
//
//        // 2. Обновить состояние саги в БД: указать, что ждем BillingCompensateEvent
//        OrderSaga saga = sagaRepository.findById(sagaId).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + sagaId));
//        saga.setStatus("WAITING"); // Новый статус
//        saga.setExpectedEventType(SagaExpectedEventType.BILLING_COMPENSATION_EVENT); // <-- Используем ENUM
//        saga.setExpectedEventOrderId(orderId); // Для быстрого поиска
//        saga.setWaitTimeoutAt(LocalDateTime.now().plusSeconds(30)); // Установить таймаут
//        sagaRepository.save(saga);
//
//        // 3. Вернуть "ожидающий" результат
//        return StepExecutionResult.waiting();
    }

//    @Override
//    public StepExecutionResult<StockReservedEvent> execute(ReserveStockCommand command) {
//        CompletableFuture<StockReservedEvent> future = new CompletableFuture<>();
//        pendingRequests.put(command.getOrder().getId(), future);
//
//        // Отправка команды в Kafka
//        try {
//            kafkaTemplate.send("warehouse.commands", command);
//
//            // Ждем результат из Kafka (с таймаутом)
//            StockReservedEvent reservedEvent = future.get(30, TimeUnit.SECONDS); // Таймаут!
//            if ("RESERVED".equals(reservedEvent.getStatus())) {
//                return StepExecutionResult.success(reservedEvent);
//            } else {
//                return StepExecutionResult.failure("Stock reservation failed with status: " + reservedEvent.getStatus());
//            }
//        } catch (TimeoutException e) {
//            return StepExecutionResult.failure("Timeout waiting for stock reservation response for order: " + command.getOrder().getId());
//        } catch (InterruptedException | ExecutionException e) {
//            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
//            return StepExecutionResult.failure("Error waiting for stock reservation response: " + e.getCause().getMessage()); // getCause() для получения реальной ошибки
//        } catch (Exception ex) {
//            return StepExecutionResult.failure("Error when sending Kafka message: " + ex.getCause().getMessage()); // getCause() для получения реальной ошибки
//        } finally {
//            pendingRequests.remove(command.getOrder().getId()); // Убираем из мапы
//        }
//    }
//
//    // Kafka Listener для получения ответа от склада
//    @KafkaListener(topics = "warehouse.responses", groupId = "order-group")
//    public void handleStockReservationResponse(@Payload StockReservedEvent event) {
//        log.info("Saga {} [{}] Received {} from warehouse.", event.getSagaId(), event.getOrderId(), "stock.reservation");
//        CompletableFuture<StockReservedEvent> future = pendingRequests.get(event.getOrderId());
//        if (future != null) {
//            future.complete(event);
////            return;
//        }
//        // Если не нашли ожидающий запрос - игнорируем.
//        // Публикуем событие в Kafka - надо заходить в сагу как положено.
//
//        OrderSaga saga = sagaRepository.findById(event.getSagaId()).orElseThrow(() -> new RuntimeException("Saga not found"));
//        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));
//
//        if (event.getStatus().equals("RESERVED")){
//            // Резервирование успешно
//            order.setStatus(OrderStatus.PENDING_RESERVATION);
//            // Переходим к следующему шагу
//            saga.setCurrentStep("DELIVERY");
//        } else {
//            // резервирование провалилось, сагу отказываем
//            log.error("Stock reservation step failed for Order {}: {}", order.getId(), "");
//            saga.setStatus("FAILED");
//        }
//
//        orderRepository.save(order);
//        sagaRepository.save(saga);
//
//
//        OrderCreatedEvent nextEvent = OrderCreatedEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getName(), event.getSagaId());
//        kafkaTemplate.send("order.saga.events", "order_warehouse_response" + order.getId(), nextEvent);

    /// /        if (event.getStatus().equals("RESERVED")) {
    /// /            kafkaTemplate.send("order.saga.events", "order_warehouse_reserved" + order.getId(), nextEvent);
    /// /        } else {
    /// /            kafkaTemplate.send("order.saga.events", "order_warehouse_failed" + order.getId(), nextEvent);
    /// /        }
//    }

}