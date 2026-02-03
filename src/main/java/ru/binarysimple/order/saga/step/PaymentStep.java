package ru.binarysimple.order.saga.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.ParentType;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.saga.events.SagaEvents;
import ru.binarysimple.order.service.OutboxService;

import static ru.binarysimple.order.model.saga.OrderSaga.SagaState.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStep implements SagaStep {

    private static final EventType EVENT_TYPE = EventType.PAYMENT_REQUESTED;

    private final OutboxService outboxService;

    private final BillingServiceClient billingClient;

    private static final OrderSaga.SagaState STEP_SAGA_STATE = OrderSaga.SagaState.PAYMENT_PROCESSING;

//    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void execute(OrderSaga saga, OrderResultDto order) {

        saga.setState(STEP_SAGA_STATE);

        OperationDto operationDto = null;

        try {
            operationDto = billingClient.makePayment(order);
//        } catch (BillingServiceException billingServiceException) {
//            return StepExecutionResult.failure(OrderStatus.INSUFFICIENT_FUNDS.name());
            saga.setState(PAYMENT_COMPLETED);
        } catch (Exception e) {
//            operationDto = new OperationDto()
            saga.setState(PAYMENT_FAILED);
        }


        SagaEvents.PaymentResponseEvent paymentEvent = SagaEvents.PaymentResponseEvent.builder()
                .sagaId(saga.getId())
                .operation(operationDto)
                .build();

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

    }

    @Override
    public boolean canHandle(OrderSaga.SagaState state) {
        return state == STEP_SAGA_STATE;
    }

    @Override
    public OrderSaga.SagaState getState() {
        return STEP_SAGA_STATE;
    }
}
