package ru.binarysimple.order.saga.events;

import lombok.Value;
import ru.binarysimple.order.dto.OperationDto;

@Value
public class PaymentProcessedEvent {
    OperationDto operationDto;
}
