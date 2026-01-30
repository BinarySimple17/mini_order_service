package ru.binarysimple.order.model;

import lombok.Getter;
import lombok.ToString;

@Getter
public enum OrderStatus {
    NEW("New"),
    PENDING_PAYMENT("Pending Payment"), // После успешного вызова Billing
    PENDING_RESERVATION("Pending Reservation"), // После успешного резерва на складе
    PENDING_DELIVERY("Pending Delivery"),    // После успешной инициализации доставки
    CONFIRMED("Confirmed"),      // После завершения всех шагов
    PAYMENT_FAILED("Payment Failed"), // Если оплата не удалась
    RESERVATION_FAILED("Reservation Failed"), // Если резерв не удался
    DELIVERY_FAILED("Delivery Failed"),    // Если доставка не удалась
    CANCELLED_DUE_TO_SAGA_ERROR("Cancelled due to Saga error"), // Если ошибка произошла и была частично/полностью скомпенсирована
    INSUFFICIENT_FUNDS("Insufficient funds"),
    FAILED("Failed"),
    IN_PROGRESS("In progress"),
    PAID("Paid"),
    DONE("Done"),
    CANCELED("Canceled"),
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
