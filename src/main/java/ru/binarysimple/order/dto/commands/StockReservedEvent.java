package ru.binarysimple.order.dto.commands;

import lombok.Value;

@Value
public class StockReservedEvent {
    Long orderId;
    String status;
    Object stockReservation;
}
