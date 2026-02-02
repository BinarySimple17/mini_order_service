package ru.binarysimple.order.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.Order;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockCommand {
    OrderResultDto order;
    UUID sagaId;
}
