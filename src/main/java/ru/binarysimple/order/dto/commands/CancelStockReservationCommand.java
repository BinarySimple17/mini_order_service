package ru.binarysimple.order.dto.commands;

import lombok.Value;

@Value
public class CancelStockReservationCommand {
    Long orderId;
    String status;
    Object stockReservation;
}
