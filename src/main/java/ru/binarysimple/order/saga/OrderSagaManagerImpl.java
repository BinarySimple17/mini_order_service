package ru.binarysimple.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.events.SagaEvents;
import ru.binarysimple.order.saga.processor.EventProcessor;
import ru.binarysimple.order.saga.processor.PaymentCompensationResponseProcessor;
import ru.binarysimple.order.saga.processor.PaymentResponseProcessor;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderSagaManagerImpl implements OrderSagaManager {

    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private final  SagaRecoveryService sagaRecoveryService;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final SagaStateMachine stateMachine;
    private final BillingServiceClient billingServiceClient;
    private final PaymentResponseProcessor paymentResponseProcessor;
    private final PaymentCompensationResponseProcessor paymentCompensationResponseProcessor;

    @Override
    @Transactional
    public void startNew(OrderResultDto order) {

        OrderSaga saga = createSaga(order);

        log.info("Started saga {} for order {}", saga.getId(), order.getId());

        stateMachine.process(saga, order);

        sagaRepository.save(saga);
    }


    private OrderSaga createSaga(OrderResultDto order) {
        OrderSaga saga = new OrderSaga();
        saga.setOrderId(order.getId());
        return sagaRepository.save(saga);
    }

    @KafkaListener(
            id = "paymentListener",
            topics = "payment.response",
            groupId = "order-service")
    @Transactional
    public void handlePaymentResponse(String message) {

        log.debug("handlePaymentResponse from kafka {}", message);
        processMessage2(paymentResponseProcessor, message, SagaEvents.PaymentResponseEvent.class);

    }

    @KafkaListener(
            id = "paymentListenerCompensation",
            topics = "payment.response.compensation",
            groupId = "order-service")
    @Transactional
    public void handlePaymentCompensationResponse(String message) {

        log.debug("handlePaymentCompensationResponse from kafka {}", message);

        processMessage2(paymentCompensationResponseProcessor, message, SagaEvents.OrderFailedEvent.class);
    }

    private <T> void processMessage2(EventProcessor<T> processor, String message, Class<T> eventClass) {
        log.info("processMessage2 event: {}", processor.getClass().getSimpleName());

        try {
            T event = objectMapper.readValue(message, eventClass);
            processor.processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process response: {}", SagaEvents.PaymentResponseEvent.class, e);
        }
    }

    @Scheduled(fixedDelayString = "${app.saga.recover-interval: 180000}")
    @Transactional
    @Override
    public void recoverStuckSagas() {
        sagaRecoveryService.recoverStuckSagas(stateMachine);
        sagaRecoveryService.compensateFailedSagas(stateMachine);
    }

}
