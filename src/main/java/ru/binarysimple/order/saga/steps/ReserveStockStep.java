package ru.binarysimple.order.saga.steps;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
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
import java.util.UUID;

import static ru.binarysimple.order.model.OrderSagaStep.BILLING;
import static ru.binarysimple.order.model.OrderSagaStep.DELIVERY;
import static ru.binarysimple.order.model.OrderStatus.PENDING_RESERVATION;
import static ru.binarysimple.order.model.OrderStatus.RESERVATION_FAILED;

@Component
@AllArgsConstructor
@Slf4j
public class ReserveStockStep implements SagaStep<ReserveStockCommand, StockReservedEvent> {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    public StepExecutionResult<StockReservedEvent> execute(ReserveStockCommand command) {
        UUID sagaId = command.getSagaId();
        Long orderId = command.getOrder().getId();

        // 1. Отправить команду в Kafka для резервирования
        kafkaTemplate.send("warehouse.commands.reserve", command.getOrder().getId().toString(), command);
//        sendWithHeader("warehouse.commands.reserve", command.getOrder().getId().toString(), command, command.getSagaId().toString());

        // 2. Обновить состояние саги в БД: указать, что ждем StockReservedEvent
        OrderSaga saga = sagaRepository.findById(sagaId).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + sagaId));
        saga.setStatus("WAITING"); // Новый статус
        saga.setExpectedEventType(SagaExpectedEventType.STOCK_RESERVED_EVENT); // <-- Используем ENUM
        saga.setExpectedEventOrderId(orderId); // Для быстрого поиска
        saga.setWaitTimeoutAt(LocalDateTime.now().plusSeconds(30)); // Установить таймаут
        sagaRepository.saveAndFlush(saga);

        // 3. Вернуть "ожидающий" результат
        return StepExecutionResult.waiting();
    }

    public void processStockResponse(StockReservedEvent event) {
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
            saga.setCompensateStep(BILLING);
            handleTimeout(saga);
            return;
        }

        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found for ID: " + event.getOrderId()));

        // 3. Проверить результат события
        if ("RESERVED".equals(event.getStatus())) {
            // 4. Обновить состояние саги: сбросить ожидание, перейти к следующему шагу
            saga.setStatus("PROCESSING"); // Снова активна
            saga.setCurrentStep(DELIVERY); // Следующий шаг
            saga.setExpectedEventType(null); // Сброс ожидания
            saga.setExpectedEventOrderId(null);
            saga.setWaitTimeoutAt(null);
            sagaRepository.saveAndFlush(saga);
            order.setStatus(PENDING_RESERVATION);
            orderRepository.saveAndFlush(order);

            // 5. Повторно запустить оркестратор
            OrderCreatedEvent nextEvent = OrderCreatedEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getSimpleName(), saga.getId());
            kafkaTemplate.send("order.saga.events", event.getOrderId().toString(), nextEvent);

        } else { // "FAILED" или другой статус ошибки
            log.error("Stock reservation failed for Order {} in Saga {}", event.getOrderId(), saga.getId());
            saga.setStatus("COMPENSATING"); // Начать компенсацию
            saga.setCompensateStep(BILLING);
            saga.setExpectedEventType(null);
            saga.setExpectedEventOrderId(null);
            saga.setWaitTimeoutAt(null);
            sagaRepository.saveAndFlush(saga);
            order.setStatus(RESERVATION_FAILED);
            orderRepository.saveAndFlush(order);
            // 5. Повторно запустить оркестратор
            OrderCompensateEvent nextEvent = OrderCompensateEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getSimpleName(), saga.getId());
            kafkaTemplate.send("order.saga.compensate", event.getOrderId().toString(), nextEvent);
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
        kafkaTemplate.send("order.saga.compensate", saga.getOrderId().toString(), nextEvent);
    }

    @Override
    public StepExecutionResult<StockReservedEvent> compensate(ReserveStockCommand command) {
        // Отправка команды отмены в Kafka
        kafkaTemplate.send("warehouse.commands.compensate", command.getOrder().getId().toString(),
                new CancelStockReservationCommand(command.getOrder().getId(),
                        "CANCEL_RESERVATION"));
        // Компенсация асинхронна, не понтно сразу успех/неуспех.
        return StepExecutionResult.waiting(); // Условный успех
    }

    @Deprecated
    private void sendWithHeader(String topic, String key, Object data, String sagaId ){

        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, key, data);
        producerRecord.headers().add("sagaId", sagaId.getBytes());
        kafkaTemplate.send(producerRecord);

//        kafkaTemplate.send(topic,key, data);
    }
}