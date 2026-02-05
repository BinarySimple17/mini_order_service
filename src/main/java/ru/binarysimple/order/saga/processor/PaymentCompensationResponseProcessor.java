package ru.binarysimple.order.saga.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.saga.events.SagaEvents;

//public class PaymentCompensationResponseProcessor implements EventProcessor<SagaEvents.OrderFailedEvent>{
//
//    @Override
//    public void processEvent(SagaEvents.OrderFailedEvent event, OrderSaga saga) {
//
//    }
//}
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationResponseProcessor implements EventProcessor<SagaEvents.OrderFailedEvent>{

    @Override
    public void processEvent(SagaEvents.OrderFailedEvent event) {

    }
}