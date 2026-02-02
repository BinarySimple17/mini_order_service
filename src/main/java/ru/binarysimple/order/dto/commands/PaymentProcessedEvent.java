package ru.binarysimple.order.dto.commands;

import lombok.Value;
import ru.binarysimple.order.dto.OperationDto;

@Value
public class PaymentProcessedEvent {
    OperationDto operationDto;
}
