package ru.binarysimple.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records",
        uniqueConstraints = {@UniqueConstraint(columnNames = "idempotency_key")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    public static final long EXPIRY_MINUTES = 24 * 60; // 24 часа
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "username")
    private String username;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = createdAt.plusMinutes(EXPIRY_MINUTES);
    }

}