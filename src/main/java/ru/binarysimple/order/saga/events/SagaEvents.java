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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentResponseEvent {

        private UUID sagaId;

        private final UUID eventId = UUID.randomUUID();

        private Boolean success;

        private String message;

        OperationDto operation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseReservationRequestEvent {
        private UUID sagaId;

        private final UUID eventId = UUID.randomUUID();

        private OrderResultDto order;

        private LocalDateTime timestamp = LocalDateTime.now();

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseReservationResponseEvent {

        private UUID eventId;
        private UUID sagaId;
        private Boolean success;
        private String message;
        private OrderResultDto order;
        private LocalDateTime timestamp;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseCompensationRequestEvent {
        private UUID sagaId;

        private final UUID eventId = UUID.randomUUID();

        private OrderResultDto order;

        private LocalDateTime timestamp = LocalDateTime.now();

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseCompensationResponseEvent {

        private UUID eventId;
        private UUID sagaId;
        private Boolean success;
        private String message;
        private OrderResultDto order;
        private LocalDateTime timestamp;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryRequestEvent {
        private UUID sagaId;

        private final UUID eventId = UUID.randomUUID();

        private OrderResultDto order;

        private LocalDateTime timestamp = LocalDateTime.now();

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryResponseEvent {

        private UUID eventId;
        private UUID sagaId;
        private Boolean success;
        private String message;
        private OrderResultDto order;
        private LocalDateTime timestamp;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryCompensationResponseEvent {

        private UUID eventId;
        private UUID sagaId;
        private Boolean success = true;
        private String message;
        private OrderResultDto order;
        private LocalDateTime timestamp;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderFailedEvent {
        private UUID sagaId;

        private Long orderId;

        private final UUID eventId = UUID.randomUUID();

        private String username;

        private String reason;

        private LocalDateTime timestamp = LocalDateTime.now();
    }
}
