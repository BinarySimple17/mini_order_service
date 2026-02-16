package ru.binarysimple.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "processed_event_id")
public class ProcessedEventId {
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

}