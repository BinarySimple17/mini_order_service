package ru.binarysimple.order.model.saga;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private SagaStep currentStep = SagaStep.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private SagaState state = SagaState.STARTED;
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;
    @CreationTimestamp
    @Column(
            name = "created_at",
            updatable = false,
            nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    public boolean isFailed() {
        return this.state == SagaState.FAILED;
    }

    public boolean isCurrentFinished() {
        return (this.state == SagaState.COMPLETED || this.state == SagaState.COMPENSATED);
    }

    public boolean isCompleted() {
        return (this.currentStep == SagaStep.DELIVERY) && isCurrentFinished();
    }

    public enum SagaState {
        STARTED,
        WAITING,
        PROCESSING,
        RESERVING,
        RESERVED,
        SCHEDULING,
        SCHEDULED,
        FAILED,
        COMPLETED,
        COMPENSATING,
        COMPENSATED
    }

    public enum SagaStep {
        PENDING,
        BILLING,
        WAREHOUSE,
        DELIVERY
    }
}