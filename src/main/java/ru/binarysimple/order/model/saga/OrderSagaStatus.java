package ru.binarysimple.order.model.saga;

public enum OrderSagaStatus {
    PENDING,
    PROCESSING,
    COMPENSATING,
    COMPENSATED,
    COMPLETED,
    FAILED,
    WAITING
}
