package ru.binarysimple.order.model.saga;

public enum OrderSagaStep {
    PENDING,
    BILLING,
    WAREHOUSE,
    DELIVERY
}
