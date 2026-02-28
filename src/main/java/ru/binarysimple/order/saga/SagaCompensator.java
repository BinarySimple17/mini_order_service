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

    private final SagaStateMachine sagaStateMachine;

    private final Integer retryMaxCount = 0;

    public void executeCompensation(OrderSaga saga, OrderResultDto order, String reason) {
        log.info("Executing compensation for saga {}: {}", saga.getId(), reason);

        if (saga.isFullyCompensated()) {
            finalCompensated(saga, order);
            return;
        }

        if (saga.getRetryCountCompensation() >= retryMaxCount) {
            finalFailed(saga, order);
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
        sagaStateMachine.compensate(saga, order, reason, EventType.DELIVERY_CANCELLED);
    }

    public void compensatePayment(OrderSaga saga, OrderResultDto order) {
        saga.setPaymentExecuted(false);
        setSagaCompensated(saga, order);
    }

    public void compensateWarehouse(OrderSaga saga, OrderResultDto order) {
        saga.setWarehouseExecuted(false);
        setSagaCompensated(saga, order);
    }

    public void compensateDelivery(OrderSaga saga, OrderResultDto order) {
        saga.setDeliveryExecuted(false);
        setSagaCompensated(saga, order);
    }

    private void setSagaCompensated(OrderSaga saga, OrderResultDto order) {
        if (saga.isFullyCompensated()) {
            finalCompensated(saga, order);
        }
    }

    private void finalCompensated(OrderSaga saga, OrderResultDto order) {
        log.info("Sending full compensation for saga {}", saga.getId());

        try {
            String compensationReason = "Fully compensated";
            sagaStateMachine.compensate(saga, order, compensationReason, EventType.FINAL_COMPENSATION);
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        } catch (Exception e) {
            log.error("FULLY Compensation failed for saga {}: {}", saga.getId(), e.getMessage());
        }
    }

    private void finalFailed(OrderSaga saga, OrderResultDto order) {
        log.info("Sending final failed compensation for saga {}", saga.getId());

        try {
            String compensationReason = "Compensation failed";
            sagaStateMachine.compensate(saga, order, compensationReason, EventType.COMPENSATION_FAILED);
            saga.setState(OrderSaga.SagaState.COMPENSATION_FAILED);
        } catch (Exception e) {
            log.error("Compensation failed for saga {}: {}", saga.getId(), e.getMessage());
        }
    }
}
