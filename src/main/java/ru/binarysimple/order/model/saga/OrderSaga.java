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
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    @Column(name = "retry_count_comp", nullable = false)
    private Integer retryCountCompensation = 0;
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

    @Getter
    public enum SagaState {
        STARTED("Started"),
        COMPENSATION_FAILED("Compensation Failed"),
        PAYMENT_PROCESSING("Payment processing"),
        PAYMENT_COMPLETED("Payment completed"),
        PAYMENT_FAILED("Payment failed"),
        WAREHOUSE_RESERVING("Warehouse stock reserving"),
        WAREHOUSE_RESERVED("Warehouse stock reserved"),
        WAREHOUSE_FAILED("Warehouse reserving failed"),
        DELIVERY_SCHEDULING("Delivery scheduling"),
        DELIVERY_SCHEDULED("Delivery scheduled"),
        DELIVERY_FAILED("Delivery failed"),
        COMPLETED("Completed"),
        COMPENSATING("Compensating"),
        COMPENSATED("Compensated");

        private final String text;

        SagaState(String text) {
            this.text = text;
        }
    }

//    public enum SagaStep {
//        PENDING,
//        BILLING,
//        WAREHOUSE,
//        DELIVERY
//    }
}