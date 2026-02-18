package ru.binarysimple.order.saga.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.ParentType;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.saga.events.SagaEvents;
import ru.binarysimple.order.service.NotificationService;
import ru.binarysimple.order.service.OutboxService;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryStep implements SagaStep {

    private static final EventType EVENT_TYPE = EventType.DELIVERY_REQUESTED;
    private static final OrderSaga.SagaState STEP_SAGA_STATE = OrderSaga.SagaState.DELIVERY_SCHEDULING;
    private final OutboxService outboxService;
    private static final EventType COMPENSATE_EVENT_TYPE = EventType.DELIVERY_CANCELLED;

    private final NotificationService notificationService;

    @Override
    @Transactional
    public void execute(OrderSaga saga, OrderResultDto order) {

        saga.setState(STEP_SAGA_STATE);

        SagaEvents.DeliveryRequestEvent event = SagaEvents.DeliveryRequestEvent.builder()
                .sagaId(saga.getId())
                .order(order)
                .build();

        outboxService.saveEvent(
                EVENT_TYPE,
                saga.getId().toString(),
                ParentType.SAGA,
                event,
                "delivery.reserve.request");

        log.info("Delivery requested for order {} via saga {}", order.getId(), saga.getId());

//        notificationService.sendNotification(saga,order, EVENT_TYPE);
    }

    @Override
    public void compensate(OrderSaga saga, OrderResultDto order, String reason) {

        SagaEvents.WarehouseCompensationRequestEvent failedEvent = SagaEvents.WarehouseCompensationRequestEvent.builder()
                .sagaId(saga.getId())
                .order(order)
                .build();

        outboxService.saveEvent(
                COMPENSATE_EVENT_TYPE,
                order.getId().toString(),
                ParentType.SAGA,
                failedEvent,
                "warehouse.compensate.request");

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
