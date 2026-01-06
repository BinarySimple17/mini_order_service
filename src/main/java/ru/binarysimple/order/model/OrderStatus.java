package ru.binarysimple.order.model;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum OrderStatus {
    NEW("New"),
    IN_PROGRESS("In progress"),
    DONE("Done"),
    CANCELED("Canceled");

    private final String title;

    OrderStatus(String title) {
        this.title = title;
    }
}
