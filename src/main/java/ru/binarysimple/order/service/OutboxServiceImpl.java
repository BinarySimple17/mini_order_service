package ru.binarysimple.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.exception.OutboxEventSaveException;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.OutboxEvent;
import ru.binarysimple.order.model.ParentType;
import ru.binarysimple.order.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Service
@Slf4j
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${app.outbox.retry-max-count:2}")
    private int retries;
    @Value("${app.outbox.send-timeout-seconds:30}")
    private int timeout;

    @Override
//    @Transactional
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEvent(EventType eventType, String parentId, ParentType parentType, Object payload, String topic) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setEventType(eventType);
            event.setParentId(parentId);
            event.setParentType(parentType);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setTopic(topic);

            outboxRepository.save(event);
            log.debug("Saved outbox event: {} for {}", eventType, parentId);
        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new OutboxEventSaveException("Failed to save outbox event", e);
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${app.outbox.interval:6000}")
//    @Transactional
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findUnpublishedEvents(retries);
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getParentId(), event.getPayload()).get();
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to relay outbox event: {}", event.getEventId(), e);
            }
//            try {
//                CompletableFuture<?> future =
//                        kafkaTemplate.send(event.getTopic(), event.getParentId(), event.getPayload());
//                future.get(timeout, java.util.concurrent.TimeUnit.SECONDS);
//                event.setPublished(true);
//                event.setPublishedAt(LocalDateTime.now());
//                event.setErrorMessage(null);
//                outboxRepository.save(event);
//
//                log.info("Published event {} to topic {}", event.getEventType(), event.getTopic());
//            } catch (InterruptedException interruptedException) {
//                onError(event, interruptedException);
//                Thread.currentThread().interrupt();
//            } catch (Exception e) {
//                onError(event, e);
//            }
        }
    }

    private void onError(OutboxEvent event, Exception e) {
        log.error("Failed to publish event {}: {}", event.getEventId(), e.getMessage());

        outboxRepository.incrementRetryCount(event.getEventId(), e.getMessage());

        if (event.getRetryCount() >= retries) {
            log.error("Event {} {} exceeded max retries, moving to DLQ not yet implemented",
                    event.getEventType(), event.getEventId());
        }
    }
}
