package ru.binarysimple.order.model;

public enum ParentType {
    ORDER("Order"),
    SAGA("Saga");

    private final String parentName;

    ParentType(String parentName) {
        this.parentName = parentName;
    }

    @Override
    public String toString() {
        return parentName;
    }
}
