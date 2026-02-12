package ru.binarysimple.order.saga.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.SagaCompensator;
import ru.binarysimple.order.saga.events.SagaEvents;

import static ru.binarysimple.order.model.OrderStatus.CANCELED;


@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationResponseProcessor implements EventProcessor<SagaEvents.OrderFailedEvent> {

    private final SagaCompensator sagaCompensator;

    private final OrderRepository orderRepository;

    private final OrderSagaRepository sagaRepository;

    private final OrderMapper orderMapper;

    @Override
    public void processEvent(SagaEvents.OrderFailedEvent event) {

        OrderSaga saga = sagaRepository.findById(event.getSagaId()).orElseThrow(() ->
                new RuntimeException("PaymentCompensationResponseProcessor saga not found " + event.getSagaId()));


        Order order = orderRepository
                .findById(saga.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
        sagaCompensator.compensatePayment(saga, orderMapper.toOrderResultDto(order));
        sagaRepository.save(saga);
        if (saga.getState() == OrderSaga.SagaState.COMPENSATED) {
            setOrderStatus(order, CANCELED);
        }
        log.info("Payment compensation successful, saga {} moved to {}", saga.getId(), saga.getState());
    }

    private void setOrderStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        orderRepository.save(order);
    }
}