package ru.binarysimple.order.model;

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
    private String currentStep; // e.g., "BILLING", "WAREHOUSE", "DELIVERY"
    private String status;      // e.g., "PROCESSING", "COMPENSATING", "COMPLETED", "FAILED"
    private LocalDateTime createdAt;
}