package ru.binarysimple.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.binarysimple.order.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}