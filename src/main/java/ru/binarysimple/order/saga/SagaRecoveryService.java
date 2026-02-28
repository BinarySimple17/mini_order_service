package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaRecoveryService {

    private final OrderMapper orderMapper;

    private final OrderSagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaCompensator sagaCompensator;

    private final Integer retryMaxCount = 3;

    private static String getErrorCompensatedMessage(OrderSaga saga) {
        return String.format("Compensation error at step [%s]", saga.getState());
    }

    public void recoverStuckSagas(SagaStateMachine stateMachine) {

        List<OrderSaga> stuckSagas = findStuckSagas(stateMachine);

        if (!stuckSagas.isEmpty()) log.warn("Found {} stuck sagas for recovery", stuckSagas.size());

        for (OrderSaga saga : stuckSagas) {

            Order order = getOrder(saga.getOrderId());

            if (saga.getRetryCount() < retryMaxCount) {
                recoverSaga(saga, stateMachine);
            } else {
                sagaCompensator.executeCompensation(
                        saga, orderMapper.toOrderResultDto(order), "Failed to recover saga after maximum retries");
            }
            sagaRepository.save(saga);

        }
    }

    public void compensateFailedSagas(SagaStateMachine stateMachine) {

        List<OrderSaga> stuckSagas = findFailedSagas(stateMachine);

        if (!stuckSagas.isEmpty()) log.warn("Found {} failed sagas for compensation", stuckSagas.size());

        for (OrderSaga saga : stuckSagas) {
            Order order = getOrder(saga.getOrderId());
            String errorMessage = getErrorCompensatedMessage(saga);
            sagaCompensator.executeCompensation(saga, orderMapper.toOrderResultDto(order), errorMessage);
            sagaRepository.save(saga);

        }
    }

    private List<OrderSaga> findFailedSagas(SagaStateMachine stateMachine) {
        // Находим саги, которые ожидают ответов
        List<OrderSaga> sagasAwaitingResponse = sagaRepository.findByStateIn(List.of(
                OrderSaga.SagaState.PAYMENT_FAILED,
                OrderSaga.SagaState.WAREHOUSE_FAILED,
                OrderSaga.SagaState.DELIVERY_FAILED,
                OrderSaga.SagaState.COMPENSATING));
        // Фильтруем те, у которых истек таймаут
        return sagasAwaitingResponse.stream()
                .filter(saga -> {
                    long timeout = stateMachine.getTimeoutForState(saga.getState());
                    return saga.getUpdatedAt().isBefore(LocalDateTime.now().minus(timeout, ChronoUnit.MILLIS));
                })
                .toList();
    }

    private List<OrderSaga> findStuckSagas(SagaStateMachine stateMachine) {
        // Находим саги, которые ожидают ответов
        List<OrderSaga> sagasAwaitingResponse = sagaRepository.findByStateIn(List.of(
                OrderSaga.SagaState.PAYMENT_PROCESSING,
                OrderSaga.SagaState.WAREHOUSE_RESERVING,
                OrderSaga.SagaState.DELIVERY_SCHEDULING));

        // Фильтруем те, у которых истек таймаут
        return sagasAwaitingResponse.stream()
                .filter(saga -> {
                    long timeout = stateMachine.getTimeoutForState(saga.getState());
                    return saga.getUpdatedAt().isBefore(LocalDateTime.now().minus(timeout, ChronoUnit.MILLIS));
                })
                .toList();
    }

    private void recoverSaga(OrderSaga saga, SagaStateMachine stateMachine) {

        Order order = getOrder(saga.getOrderId());

        try {
            saga.setRetryCount(saga.getRetryCount() + 1);
            log.warn(
                    "Recovering saga {} in state {} (retry {}/3)",
                    saga.getId(),
                    saga.getState(),
                    saga.getRetryCount());

            stateMachine.retryStep(saga, orderMapper.toOrderResultDto(order));

        } catch (Exception e) {
            log.error("Failed to recover saga {}", saga.getId(), e);
        }
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}
