package ru.binarysimple.order.model.saga;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "order_saga")
public class OrderSaga {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private OrderSagaStep currentStep; // e.g., "BILLING", "WAREHOUSE", "DELIVERY"

    @Enumerated(EnumType.STRING)
    private OrderSagaStep compensateStep; // e.g., "BILLING", "WAREHOUSE", "DELIVERY"

    @Enumerated(EnumType.STRING)
    private OrderSagaStatus status;      // e.g., "PROCESSING", "COMPENSATING", "COMPLETED", "FAILED", "WAITING"

    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_event_type")
    private SagaExpectedEventType expectedEventType;    //событие, которое ждем

    @Column(name = "expected_event_order_id")
    private Long expectedEventOrderId; // orderId, для которого ждем событие

    @Column(name = "wait_timeout_at")
    private LocalDateTime waitTimeoutAt; // Время, до которого ждем ответ
    // --------------------------------------------

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}