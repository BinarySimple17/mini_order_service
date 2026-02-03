package ru.binarysimple.order.saga.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.OrderResultDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


public class SagaEvents {

//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    public static class PaymentRequestEvent {
//        private UUID sagaId;
//
//        OrderResultDto order;
//
//        private LocalDateTime timestamp = LocalDateTime.now();
//    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentResponseEvent {

        private UUID sagaId;

        OperationDto operation;
    }
}
