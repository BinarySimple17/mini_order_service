package ru.binarysimple.order.saga.processor;

public interface EventProcessor <Event, Result>{

    ProcessorResult<Result> processEvent(Event event);
}
