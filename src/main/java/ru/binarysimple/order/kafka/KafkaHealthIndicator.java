package ru.binarysimple.order.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaHealthIndicator(KafkaTemplate<String, String> kafkaTemplate,
                                @Value("${app.kafka.topics.order-events:order.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public Health health() {
        try {
            if (isKafkaReachable()) {
                return Health.up()
                    .withDetail("Kafka broker", "reachable")
                    .withDetail("test topic", topic)
                    .build();
            } else {
                return Health.down()
                    .withDetail("Kafka broker", "not reachable")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private boolean isKafkaReachable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] success = {false};

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return kafkaTemplate.send(topic, "health-check", "ping").get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        future.thenAccept(result -> {
            success[0] = true;
            latch.countDown();
        }).exceptionally(ex -> {
            success[0] = false;
            latch.countDown();
            return null;
        });

        // Ждём до 3 секунд
        boolean waited = latch.await(3, TimeUnit.SECONDS);
        return waited && success[0];
    }
}

