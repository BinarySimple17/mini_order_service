package ru.binarysimple.order.model;

public enum OrderSagaStep {
    PENDING,
    BILLING,
    WAREHOUSE,
    DELIVERY
}
