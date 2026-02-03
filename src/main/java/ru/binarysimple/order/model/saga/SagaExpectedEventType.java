package ru.binarysimple.order.model.saga;


public enum SagaExpectedEventType {
    STOCK_RESERVED_EVENT("StockReservedEvent"),
    PAYMENT_REQUESTED_EVENT("PaymentRequested"),
    PAYMENT_MADE_EVENT("PaymentMade"),
    BILLING_COMPENSATION_EVENT("BillingCompensationRequested"),
    PAYMENT_PROCESSED_EVENT("PaymentProcessedEvent"), // Если когда-либо понадобится ожидать события от оплаты
    DELIVERY_INITIATED_EVENT("DeliveryInitiatedEvent"); //

    private final String eventName;

    SagaExpectedEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}