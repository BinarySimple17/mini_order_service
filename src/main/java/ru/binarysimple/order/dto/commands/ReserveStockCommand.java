package ru.binarysimple.order.dto.commands;

import lombok.Value;
import ru.binarysimple.order.model.Order;

@Value
public class ReserveStockCommand {
    Order order;
}
