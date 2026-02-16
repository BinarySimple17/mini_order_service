package ru.binarysimple.order.saga.processor;

import ru.binarysimple.order.model.saga.OrderSaga;

public interface EventProcessor <Event>{

    void processEvent(Event event);
//    void processEvent(Event event, OrderSaga saga);
}
