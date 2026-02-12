package ru.binarysimple.order.saga.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.SagaStateMachine;
import ru.binarysimple.order.saga.events.SagaEvents;
import ru.binarysimple.order.service.NotificationService;

import static ru.binarysimple.order.model.OrderStatus.DELIVERY_RESERVED;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryResponseProcessor implements EventProcessor<SagaEvents.DeliveryResponseEvent> {

    private final OrderSagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaStateMachine stateMachine;

    private final OrderMapper orderMapper;

    private final NotificationService notificationService;

    @Override
    public void processEvent(SagaEvents.DeliveryResponseEvent event) {

        OrderSaga saga = sagaRepository.findById(event.getSagaId()).orElseThrow(() -> new RuntimeException("Saga not found for ID: " + event.getSagaId()));

        if (event.getSuccess()) {
            Order order = orderRepository
                    .findById(saga.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
            order.setStatus(DELIVERY_RESERVED);
            orderRepository.save(order);
            saga.setDeliveryExecuted(true);
            saga.setState(OrderSaga.SagaState.DELIVERY_SCHEDULED);
            sagaRepository.save(saga);

            stateMachine.process(saga, orderMapper.toOrderResultDto(order));
            sagaRepository.save(saga);
            log.info("Delivery reserving successful, saga {} moved to completed", saga.getId());

            notificationService.sendNotification(saga, orderMapper.toOrderResultDto(order), EventType.FINAL_NOTIFICATION);

        } else {
            saga.setDeliveryExecuted(false);
            saga.setState(OrderSaga.SagaState.DELIVERY_FAILED);
//            saga.setErrorMessage("Payment failed: " + event.getMessage());
            sagaRepository.save(saga);
        }
    }
}
