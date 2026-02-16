package ru.binarysimple.order.dto.commands;

import lombok.Value;
import ru.binarysimple.order.model.Order;

@Value
public class MakePaymentCommand {
    Order order;
}
