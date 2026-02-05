package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.service.OutboxService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCompensator {

    private final OutboxService outboxService;

//    private final KafkaTopicProperties kafkaTopicProperties;

    private final SagaStateMachine sagaStateMachine;

    public void executeCompensation(OrderSaga saga, OrderResultDto order, String reason) {
        log.info("Executing compensation for saga {}: {}", saga.getId(), reason);

        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
            return;
        }

        saga.setState(OrderSaga.SagaState.COMPENSATING);

        if (saga.isPaymentExecuted()) {
            sendPaymentCompensate(saga, order, reason);
        }
        if (saga.isWarehouseExecuted()) {
            sendWarehouseCompensate(saga, order, reason);
        }
        if (saga.isDeliveryExecuted()) {
            sendDeliveryCompensate(saga, order, reason);
        }

    }

    private void sendPaymentCompensate(OrderSaga saga, OrderResultDto order, String reason) {
        sagaStateMachine.compensate(saga, order, reason, EventType.PAYMENT_REFUNDED);
    }

    private void sendWarehouseCompensate(OrderSaga saga, OrderResultDto order, String reason) {
        sagaStateMachine.compensate(saga, order, reason, EventType.WAREHOUSE_CANCELED);
    }

    private void sendDeliveryCompensate(OrderSaga saga, OrderResultDto order, String reason) {
//        SagaEvents.OrderFailedEvent failedEvent = SagaEvents.OrderFailedEvent.builder()
//                .sagaId(saga.getId())
//                .orderId(order.getId())
//                .userId(order.getUsername())
//                .reason(reason)
//                .build();
//
//        outboxService.saveEvent(
//                EventType.DELIVERY_CANCELLED,
//                order.getId().toString(),
//                ParentType.SAGA,
//                failedEvent,
//                "delivery.request.compensation");
    }

    public void compensatePayment(OrderSaga saga) {
        saga.setPaymentExecuted(false);
        setSagaCompensated(saga);
    }

    public void compensateWarehouse(OrderSaga saga) {
        saga.setWarehouseExecuted(false);
        setSagaCompensated(saga);
    }

    public void compensateDelivery(OrderSaga saga) {
        saga.setDeliveryExecuted(false);
        setSagaCompensated(saga);
    }

    private void setSagaCompensated(OrderSaga saga) {
        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        }
    }
}
