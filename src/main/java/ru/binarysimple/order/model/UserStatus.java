package ru.binarysimple.order.model;

import lombok.Getter;

@Getter
public enum UserStatus {
    INITIAL(null),
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private final String status;

    UserStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}
