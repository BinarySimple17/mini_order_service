package ru.binarysimple.order.saga.steps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.dto.commands.CancelStockReservationCommand;
import ru.binarysimple.order.dto.commands.ReserveStockCommand;
import ru.binarysimple.order.dto.commands.StockReservedEvent;
import ru.binarysimple.order.saga.SagaStep;
import ru.binarysimple.order.saga.StepExecutionResult;

import java.util.Map;
import java.util.concurrent.*;

@Component
public class ReserveStockStep implements SagaStep<ReserveStockCommand, StockReservedEvent> {

    private final Map<Long, CompletableFuture<StockReservedEvent>> pendingRequests = new ConcurrentHashMap<>();
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private ApplicationEventPublisher eventPublisher; // Для получения результата из Kafka

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
    @KafkaListener(topics = "warehouse.responses")
    public void handleStockReservationResponse(StockReservedEvent event) {
        CompletableFuture<StockReservedEvent> future = pendingRequests.get(event.getOrderId());
        if (future != null) {
            future.complete(event);
        }
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