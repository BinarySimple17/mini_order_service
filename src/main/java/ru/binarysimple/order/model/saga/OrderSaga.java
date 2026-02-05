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

    // Битовые флаги для шагов
    private static final int PAYMENT_EXECUTED_BIT = 0;
    private static final int WAREHOUSE_EXECUTED_BIT = 1;
    private static final int DELIVERY_EXECUTED_BIT = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
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
    @Column(name = "step_flag")
    private Integer stepFlag = 0;

//    public boolean isFailed() {
//        return this.state == SagaState.FAILED;
//    }
//
//    public boolean isCurrentFinished() {
//        return (this.state == SagaState.COMPLETED || this.state == SagaState.COMPENSATED);
//    }

//    public boolean isCompleted() {
//        return (this.currentStep == SagaStep.DELIVERY) && isCurrentFinished();
//    }

    // Методы для получения состояния битов
    public boolean isPaymentExecuted() {
        return (this.stepFlag & (1 << PAYMENT_EXECUTED_BIT)) != 0;
    }

    // Методы для установки битов
    public void setPaymentExecuted(boolean executed) {
        if (executed) {
            this.stepFlag |= (1 << PAYMENT_EXECUTED_BIT);
        } else {
            this.stepFlag &= ~(1 << PAYMENT_EXECUTED_BIT);
        }
    }

    public boolean isWarehouseExecuted() {
        return (this.stepFlag & (1 << WAREHOUSE_EXECUTED_BIT)) != 0;
    }

    public void setWarehouseExecuted(boolean executed) {
        if (executed) {
            this.stepFlag |= (1 << WAREHOUSE_EXECUTED_BIT);
        } else {
            this.stepFlag &= ~(1 << WAREHOUSE_EXECUTED_BIT);
        }
    }

    public boolean isDeliveryExecuted() {
        return (this.stepFlag & (1 << DELIVERY_EXECUTED_BIT)) != 0;
    }

    public void setDeliveryExecuted(boolean executed) {
        if (executed) {
            this.stepFlag |= (1 << DELIVERY_EXECUTED_BIT);
        } else {
            this.stepFlag &= ~(1 << DELIVERY_EXECUTED_BIT);
        }
    }

    public boolean isFullyCompensated() {
        return stepFlag == 0;
    }

    public enum SagaState {
        STARTED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        WAREHOUSE_RESERVING,
        WAREHOUSE_RESERVED,
        WAREHOUSE_FAILED,
        DELIVERY_SCHEDULING,
        DELIVERY_SCHEDULED,
        DELIVERY_FAILED,
        COMPLETED,
        COMPENSATING,
        COMPENSATED
    }

//    public enum SagaStep {
//        PENDING,
//        BILLING,
//        WAREHOUSE,
//        DELIVERY
//    }
}