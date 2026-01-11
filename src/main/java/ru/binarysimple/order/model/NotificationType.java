package ru.binarysimple.order.model;

public enum NotificationType {
    SUCCESS("Success"),
    FAIL("Fail"),
    DEFAULT("Default");

    private final String typeName;

    NotificationType(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
