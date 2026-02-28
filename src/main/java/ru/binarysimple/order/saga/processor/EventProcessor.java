package ru.binarysimple.order.saga.processor;

public interface EventProcessor<Event> {

    void processEvent(Event event);
//    void processEvent(Event event, OrderSaga saga);
}
