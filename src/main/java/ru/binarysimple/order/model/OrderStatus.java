package ru.binarysimple.order.model;

import lombok.Getter;
import lombok.ToString;

@Getter
public enum OrderStatus {
    NEW("New"),
    DELIVERY_FAILED("Delivery failed"),
    INSUFFICIENT_FUNDS("Insufficient funds"),
    FAILED("Failed"),
    IN_PROGRESS("In progress"),
    PAID("Paid"),
    WAREHOUSE_RESERVED("Warehouse reserved"),
    DONE("Done"),
    CANCELED("Canceled"),
    COMPENSATED("Compensated"),
    RESERVING_PAYMENT("Reserving payment");

    private final String title;

    OrderStatus(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }
}
