package ru.binarysimple.order.saga.steps;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.dto.commands.CancelStockReservationCommand;
import ru.binarysimple.order.dto.commands.ReserveStockCommand;
import ru.binarysimple.order.saga.events.StockReservedEvent;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.SagaStep;
import ru.binarysimple.order.saga.StepExecutionResult;
import ru.binarysimple.order.saga.events.OrderCreatedEvent;

import java.util.Map;
import java.util.concurrent.*;

@Component
@AllArgsConstructor
@Slf4j
public class ReserveStockStep implements SagaStep<ReserveStockCommand, StockReservedEvent> {

    private final Map<Long, CompletableFuture<StockReservedEvent>> pendingRequests = new ConcurrentHashMap<>();
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public StepExecutionResult<StockReservedEvent> execute(ReserveStockCommand command) {
        CompletableFuture<StockReservedEvent> future = new CompletableFuture<>();
        pendingRequests.put(command.getOrder().getId(), future);

        // Отправка команды в Kafka
        try {
            kafkaTemplate.send("warehouse.commands", command);

            // Ждем результат из Kafka (с таймаутом)
            StockReservedEvent reservedEvent = future.get(30, TimeUnit.SECONDS); // Таймаут!
            if ("RESERVED".equals(reservedEvent.getStatus())) {
                return StepExecutionResult.success(reservedEvent);
            } else {
                return StepExecutionResult.failure("Stock reservation failed with status: " + reservedEvent.getStatus());
            }
        } catch (TimeoutException e) {
            return StepExecutionResult.failure("Timeout waiting for stock reservation response for order: " + command.getOrder().getId());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
            return StepExecutionResult.failure("Error waiting for stock reservation response: " + e.getCause().getMessage()); // getCause() для получения реальной ошибки
        } catch (Exception ex) {
            return StepExecutionResult.failure("Error when sending Kafka message: " + ex.getCause().getMessage()); // getCause() для получения реальной ошибки
        } finally {
            pendingRequests.remove(command.getOrder().getId()); // Убираем из мапы
        }
    }

    // Kafka Listener для получения ответа от склада
    @KafkaListener(topics = "warehouse.responses", groupId = "order-group")
    public void handleStockReservationResponse(@Payload StockReservedEvent event) {
        log.info("Saga {} [{}] Received {} from warehouse.", event.getSagaId(), event.getOrderId(), "stock.reservation");
        CompletableFuture<StockReservedEvent> future = pendingRequests.get(event.getOrderId());
        if (future != null) {
            future.complete(event);
//            return;
        }
        // Если не нашли ожидающий запрос - игнорируем.
        // Публикуем событие в Kafka - надо заходить в сагу как положено.

        OrderSaga saga = sagaRepository.findById(event.getSagaId()).orElseThrow(() -> new RuntimeException("Saga not found"));
        Order order = orderRepository.findById(event.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));

        if (event.getStatus().equals("RESERVED")){
            // Резервирование успешно
            order.setStatus(OrderStatus.PENDING_RESERVATION);
            // Переходим к следующему шагу
            saga.setCurrentStep("DELIVERY");
        } else {
            // резервирование провалилось, сагу отказываем
            log.error("Stock reservation step failed for Order {}: {}", order.getId(), "");
            saga.setStatus("FAILED");
        }

        orderRepository.save(order);
        sagaRepository.save(saga);


        OrderCreatedEvent nextEvent = OrderCreatedEvent.create(orderMapper.toOrderResultDto(order), this.getClass().getName(), event.getSagaId());
        kafkaTemplate.send("order.saga.events", "order_warehouse_response" + order.getId(), nextEvent);
//        if (event.getStatus().equals("RESERVED")) {
//            kafkaTemplate.send("order.saga.events", "order_warehouse_reserved" + order.getId(), nextEvent);
//        } else {
//            kafkaTemplate.send("order.saga.events", "order_warehouse_failed" + order.getId(), nextEvent);
//        }
    }


    @Override
    public StepExecutionResult<StockReservedEvent> compensate(ReserveStockCommand command) {
        // Отправка команды отмены в Kafka
        kafkaTemplate.send("warehouse.commands", new CancelStockReservationCommand(command.getOrder().getId(),
                "CANCEL_RESERVATION", command.getOrder()));
        // Компенсация асинхронна, не понтно сразу успех/неуспех.
        // пожтому условный успех или надо использовать более сложную логику ожидания подтверждения отмены.
        // пока и так сойдет
        return StepExecutionResult.success(null); // Условный успех
    }
}