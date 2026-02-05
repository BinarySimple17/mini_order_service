package ru.binarysimple.order.saga.step;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.ParentType;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.saga.events.SagaEvents;
import ru.binarysimple.order.service.OutboxService;

import static ru.binarysimple.order.model.saga.OrderSaga.SagaState.PAYMENT_COMPLETED;
import static ru.binarysimple.order.model.saga.OrderSaga.SagaState.PAYMENT_FAILED;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStep implements SagaStep {

    private static final EventType EVENT_TYPE = EventType.PAYMENT_REQUESTED;
    private static final EventType COMPENSATE_EVENT_TYPE = EventType.PAYMENT_REFUNDED;
    private static final OrderSaga.SagaState STEP_SAGA_STATE = OrderSaga.SagaState.PAYMENT_PROCESSING;
    private final OutboxService outboxService;
    private final BillingServiceClient billingClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
//    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
//    @Transactional
    public void execute(OrderSaga saga, OrderResultDto order) {

        saga.setState(STEP_SAGA_STATE);

        OperationDto operationDto = null;

        try {
            operationDto = billingClient.makePayment(order);
//        } catch (BillingServiceException billingServiceException) {
//            return StepExecutionResult.failure(OrderStatus.INSUFFICIENT_FUNDS.name());
//            saga.setState(PAYMENT_COMPLETED);
        } catch (Exception e) {
//            operationDto = new OperationDto()
//            saga.setState(PAYMENT_FAILED);
        }


        SagaEvents.PaymentResponseEvent paymentEvent = SagaEvents.PaymentResponseEvent.builder()
                .sagaId(saga.getId())
                .success(operationDto != null)
                .operation(operationDto)
                .build();

//        try {
//            kafkaTemplate.send("payment.response", paymentEvent.getSagaId().toString(), paymentEvent).get();
//        } catch (Exception e) {
//            log.error("Failed to send payment.response", e);
//            throw new OutboxEventSaveException("Failed to send payment.response", e);
//        }

        outboxService.saveEvent(
                EVENT_TYPE,
                saga.getId().toString(),
                ParentType.SAGA,
                paymentEvent,
                "payment.response");

        log.info("Payment requested for order {} via saga {}", order.getId(), saga.getId());
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

        outboxService.saveEvent(
                COMPENSATE_EVENT_TYPE,
                order.getId().toString(),
                ParentType.SAGA,
                failedEvent,
                "payment.response.compensation");
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
