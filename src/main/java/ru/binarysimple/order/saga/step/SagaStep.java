package ru.binarysimple.order.saga.step;

import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.saga.OrderSaga;

public interface SagaStep {

    void execute(OrderSaga saga, OrderResultDto order);

    void compensate(OrderSaga saga, OrderResultDto order, String reason);

    boolean canHandle(OrderSaga.SagaState state);

    OrderSaga.SagaState getState();

    EventType getStepCompensateEventType();
}
