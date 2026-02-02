package ru.binarysimple.order.dto.commands;

import lombok.Value;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.Order;

import java.util.UUID;

@Value
public class ReserveStockCommand {
    OrderResultDto order;
    UUID sagaId;
}
