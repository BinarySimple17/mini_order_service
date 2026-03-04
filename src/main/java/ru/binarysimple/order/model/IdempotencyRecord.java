package ru.binarysimple.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records", 
       uniqueConstraints = {@UniqueConstraint(columnNames = "idempotency_key")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "username")
    private String username;

    @Column(name = "total_cost", precision = 19, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "order_status", length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static final long EXPIRY_MINUTES = 24 * 60; // 24 часа

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = createdAt.plusMinutes(EXPIRY_MINUTES);
    }
}