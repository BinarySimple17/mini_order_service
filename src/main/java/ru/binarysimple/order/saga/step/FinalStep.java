package ru.binarysimple.order.saga.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.service.NotificationService;
import ru.binarysimple.order.service.OutboxService;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinalStep implements SagaStep {

    private static final EventType EVENT_TYPE = EventType.FINAL_NOTIFICATION;
    private static final OrderSaga.SagaState STEP_SAGA_STATE = OrderSaga.SagaState.COMPLETED;
    private final OutboxService outboxService;
    private static final EventType COMPENSATE_EVENT_TYPE = EventType.FINAL_COMPENSATION;

    private final NotificationService notificationService;

    @Override
    @Transactional
    public void execute(OrderSaga saga, OrderResultDto order) {

        saga.setState(STEP_SAGA_STATE);

//        notificationService.sendNotification(saga,order, EVENT_TYPE);
    }

    @Override
    public void compensate(OrderSaga saga, OrderResultDto order, String reason) {

//        notificationService.sendNotification(saga,order, COMPENSATE_EVENT_TYPE);
    }

    @Override
    public boolean canHandle(OrderSaga.SagaState state) {
        return state == STEP_SAGA_STATE;
    }

    @Override
    public OrderSaga.SagaState getState() {
        return STEP_SAGA_STATE;
    }

    @Override
    public EventType getStepCompensateEventType() {
        return COMPENSATE_EVENT_TYPE;
    }
}
