package ru.binarysimple.order.saga.events;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import ru.binarysimple.order.dto.OrderResultDto;

import java.util.UUID;

@Data
@Builder // Включает @AllArgsConstructor
@Jacksonized
public class OrderCreatedEvent {

    private final OrderResultDto order;
    private final String source;
    private final UUID sagaId;
    private final String eventId; // Уникальный ID события
    private final long timestamp;

    // Кастомный builder метод для удобства создания *нового* события
    public static OrderCreatedEvent create(OrderResultDto order, String source, UUID sagaId) {
        return OrderCreatedEvent.builder()
                .order(order)
                .source(source)
                .sagaId(sagaId)
                .eventId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}