package ru.binarysimple.order.exception;

public class SagaException extends RuntimeException {
    public SagaException(String message, Exception e) {
        super(message, e);
    }
}
