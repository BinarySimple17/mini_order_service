package ru.binarysimple.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String eventId;

    @Column(name = "parent_id", nullable = false)
    private String parentId; // id чье сообщение

    @Column(name = "parent_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ParentType parentType; //чье сообщение

//    @Column(name = "correlation_id")
//    private String correlationId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType; //

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "topic")
    private String topic;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(
            name = "created_at",
            updatable = false,
            nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
