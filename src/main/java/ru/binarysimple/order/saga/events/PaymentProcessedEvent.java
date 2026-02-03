package ru.binarysimple.order.saga.events;

import lombok.Value;
import ru.binarysimple.order.dto.OperationDto;

import java.util.UUID;

@Value
public class PaymentProcessedEvent {
//    UUID sagaId;
//    Long orderId;
    OperationDto operationDto;
}
