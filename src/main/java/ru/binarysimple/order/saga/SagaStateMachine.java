package ru.binarysimple.order.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.saga.step.SagaStep;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SagaStateMachine {

    private final Map<OrderSaga.SagaState, StateConfig> stateConfig;
    private final Map<OrderSaga.SagaState, SagaStep> steps;
    private final Map<EventType, SagaStep> compensationSteps;
    private final Map<OrderSaga.SagaState, OrderSaga.SagaState> transitions = Map.of(
            OrderSaga.SagaState.STARTED, OrderSaga.SagaState.PAYMENT_PROCESSING,
            OrderSaga.SagaState.PAYMENT_COMPLETED, OrderSaga.SagaState.WAREHOUSE_RESERVING,
            OrderSaga.SagaState.WAREHOUSE_RESERVED, OrderSaga.SagaState.DELIVERY_SCHEDULING,
            OrderSaga.SagaState.DELIVERY_SCHEDULED, OrderSaga.SagaState.COMPLETED);

    public SagaStateMachine(List<SagaStep> steps) {
        this.steps =
                steps.stream().collect(Collectors.toMap(SagaStep::getState, Function.identity()));

        this.compensationSteps =
                steps.stream().collect(Collectors.toMap(SagaStep::getStepCompensateEventType, Function.identity()));


        this.stateConfig = Map.of(
                OrderSaga.SagaState.STARTED, new StateConfig(false, 0L, true),
                OrderSaga.SagaState.PAYMENT_PROCESSING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.WAREHOUSE_RESERVING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.DELIVERY_SCHEDULING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.COMPLETED, new StateConfig(true, 0L, false),
                OrderSaga.SagaState.COMPENSATED, new StateConfig(true, 0L, false),
                OrderSaga.SagaState.COMPENSATING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.PAYMENT_FAILED, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.WAREHOUSE_FAILED, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.DELIVERY_FAILED, new StateConfig(false, 10_000L, false));

        log.debug("SagaStateMachine initialized with {} handlers", steps.size());
    }

    @Transactional
    public void process(OrderSaga saga, OrderResultDto order) {
//        saga.setRetryCount(0);
        OrderSaga.SagaState nextState = transitions.get(saga.getState()); //getNextState(saga.getState());
        executeStep(saga, order, nextState);
    }

    @Transactional
    public void compensate(OrderSaga saga, OrderResultDto order, String reason, EventType eventType) {
        SagaStep step = compensationSteps.get(eventType);

        if (step == null) {
//            throw new IllegalStateException("No handler for compensate eventType: " + eventType.toString());
            log.error("o handler for compensate eventType: {}", eventType);
            return;
        }

        log.debug("Executing compensate {} for saga {}", eventType.toString(), saga.getId());
        step.compensate(saga, order, reason);
    }

    private void executeStep(OrderSaga saga, OrderResultDto order, OrderSaga.SagaState nexState) {
        SagaStep step = steps.get(nexState);

        if (step == null) {
            //todo increase retry count?
//            log.error("No handler for nexState: {}",nexState);
//            return;
            throw new IllegalStateException("No handler for nexState: " + nexState);
        }

        log.debug("Executing {} for saga {}", nexState, saga.getId());
        step.execute(saga, order);
    }

    private StateConfig getConfig(OrderSaga.SagaState state) {
        return stateConfig.getOrDefault(state, new StateConfig(true, 60_000L, false));
    }

    private boolean isFinalState(OrderSaga.SagaState state) {
        return getConfig(state).finalState();
    }

    public long getTimeoutForState(OrderSaga.SagaState state) {
        return getConfig(state).timeoutMs();
    }

    @Transactional
    public void retryStep(OrderSaga saga, OrderResultDto order) {
        executeStep(saga, order, saga.getState());
    }

    private record StateConfig(boolean finalState, long timeoutMs, boolean autoTransition) {
    }
}
