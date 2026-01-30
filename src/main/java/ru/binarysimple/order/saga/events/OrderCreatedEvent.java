package ru.binarysimple.order.saga.events;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.binarysimple.order.dto.OrderResultDto;

import java.util.UUID;

@Data
@RequiredArgsConstructor
public class OrderCreatedEvent {
    private final OrderResultDto order;
    private final String source;
    private final UUID sagaId;
    private final String eventId = UUID.randomUUID().toString(); // Уникальный ID события
    private final long timestamp = System.currentTimeMillis();

}