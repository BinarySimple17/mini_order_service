package ru.binarysimple.order.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {
    Long orderId;
    String status;
    UUID sagaId;
    Long timestamp;
//    Object stockReservation;
}
