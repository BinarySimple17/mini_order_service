package ru.binarysimple.order.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelStockReservationCommand {
    Long orderId;
    String status;
//    Object stockReservation;
}
