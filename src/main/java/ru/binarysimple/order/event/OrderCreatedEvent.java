package ru.binarysimple.order.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.binarysimple.order.model.Order;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class OrderCreatedEvent {
    private final Order order;
    private final String source;
    private final String eventId = UUID.randomUUID().toString(); // Уникальный ID события
    private final long timestamp = System.currentTimeMillis();
}