package ru.binarysimple.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.SagaExpectedEventType;

import java.util.Optional;
import java.util.UUID;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {
    Optional<OrderSaga> findByExpectedEventOrderIdAndExpectedEventTypeAndStatus(Long orderId, SagaExpectedEventType eventType, String status);

}