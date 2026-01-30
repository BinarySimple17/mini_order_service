package ru.binarysimple.order.dto.commands;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import ru.binarysimple.order.model.Order;

@Value
public class CancelPaymentCommand {
    Order order;
}
