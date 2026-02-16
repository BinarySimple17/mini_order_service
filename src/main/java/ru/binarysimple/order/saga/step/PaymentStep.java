package ru.binarysimple.order.saga.step;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OperationDto;
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
public class PaymentStep implements SagaStep {

    private static final EventType EVENT_TYPE = EventType.PAYMENT_REQUESTED;
    private static final EventType COMPENSATE_EVENT_TYPE = EventType.PAYMENT_REFUNDED;
    private static final OrderSaga.SagaState STEP_SAGA_STATE = OrderSaga.SagaState.PAYMENT_PROCESSING;
    private final OutboxService outboxService;
    private final BillingServiceClient billingClient;
    private final NotificationService notificationService;

    @Override
    public void execute(OrderSaga saga, OrderResultDto order) {

        saga.setState(STEP_SAGA_STATE);

        OperationDto operationDto = null;

        try {
            operationDto = billingClient.makePayment(order);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

        SagaEvents.PaymentResponseEvent paymentEvent = SagaEvents.PaymentResponseEvent.builder()
                .sagaId(saga.getId())
                .success(operationDto != null)
                .operation(operationDto)
                .build();

        outboxService.saveEvent(
                EVENT_TYPE,
                saga.getId().toString(),
                ParentType.SAGA,
                paymentEvent,
                "payment.response");

        log.info("Payment requested for order {} via saga {}", order.getId(), saga.getId());

        notificationService.sendNotification(saga,order, EVENT_TYPE);
    }

    @Override
    public void compensate(OrderSaga saga, OrderResultDto order, String reason) {
        log.info("Payment compensate for order {} via saga {}", order.getId(), saga.getId());

        SagaEvents.OrderFailedEvent failedEvent = SagaEvents.OrderFailedEvent.builder()
                .sagaId(saga.getId())
                .orderId(order.getId())
                .username(order.getUsername())
                .reason(reason)
                .build();

        OperationDto operationDto = null;
        try {
            operationDto = billingClient.cancelPayment(order);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

        outboxService.saveEvent(
                COMPENSATE_EVENT_TYPE,
                saga.getId().toString(),    //parent id
                ParentType.SAGA,
                failedEvent,
                "payment.response.compensation");

        notificationService.sendNotification(saga,order, COMPENSATE_EVENT_TYPE);
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
