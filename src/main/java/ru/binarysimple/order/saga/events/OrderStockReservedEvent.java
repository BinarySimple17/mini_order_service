package ru.binarysimple.order.saga.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.binarysimple.order.dto.OrderResultDto;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class OrderStockReservedEvent {
    private final Long orderId;
    private final String source;
    private final UUID sagaId;
    private final String eventId;
    private final long timestamp;
}