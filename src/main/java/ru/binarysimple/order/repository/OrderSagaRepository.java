package ru.binarysimple.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.binarysimple.order.model.OrderSaga;

import java.util.UUID;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {
}