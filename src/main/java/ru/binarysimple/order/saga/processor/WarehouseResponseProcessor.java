package ru.binarysimple.order.saga.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.SagaStateMachine;
import ru.binarysimple.order.saga.events.SagaEvents;

import static ru.binarysimple.order.model.OrderStatus.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WarehouseResponseProcessor implements EventProcessor<SagaEvents.WarehouseReservationResponseEvent>{

    private final OrderSagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaStateMachine stateMachine;

    private final OrderMapper orderMapper;

    @Override
    public void processEvent(SagaEvents.WarehouseReservationResponseEvent event) {

        OrderSaga saga = sagaRepository.findById(event.getSagaId()).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + event.getSagaId()));

        if (event.getSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            order.setStatus(WAREHOUSE_RESERVED);
            orderRepository.save(order);
            saga.setWarehouseExecuted(true);
            saga.setState(OrderSaga.SagaState.WAREHOUSE_RESERVED);
            sagaRepository.save(saga);

            stateMachine.process(saga, orderMapper.toOrderResultDto(order));
            sagaRepository.save(saga);
            log.info("Warehouser reserving successful, saga {} moved to delivery", saga.getId());
        } else {
            saga.setWarehouseExecuted(false);
            saga.setState(OrderSaga.SagaState.WAREHOUSE_FAILED);
//            saga.setErrorMessage("Payment failed: " + event.getMessage());
            sagaRepository.save(saga);
        }
    }
}
