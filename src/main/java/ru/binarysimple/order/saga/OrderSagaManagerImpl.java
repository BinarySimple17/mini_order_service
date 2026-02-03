package ru.binarysimple.order.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.exception.SagaException;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.events.SagaEvents;
import ru.binarysimple.order.saga.processor.EventProcessor;
import ru.binarysimple.order.saga.processor.ProcessorResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderSagaManagerImpl implements OrderSagaManager {

    private final OrderRepository orderRepository;
    private final OrderSagaRepository sagaRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final SagaStateMachine stateMachine;
    private final BillingServiceClient billingServiceClient;

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
        log.debug("Received from kafka");
        log.debug(message);
        log.debug("-------------------");
        try {
            SagaEvents.PaymentResponseEvent event = objectMapper.readValue(message, SagaEvents.PaymentResponseEvent.class);
            log.debug("--------ok-----------");
            log.debug(event.toString());
            log.debug("-------------------");
        } catch (Exception e) {
            log.error("Failed to process {} response: {}", SagaEvents.PaymentResponseEvent.class, message, e);
        }
//        processMessage(message, PaymentResponseEvent.class, paymentResponseHandler::processPaymentResponse);
    }

    private <E> void processMessage(EventProcessor<E, ?> processor, E event) {
        log.info("Processing event: {}", processor.getClass().getSimpleName());

        ProcessorResult<?> processResult = processor.processEvent(event);
    }

//    private <T> void processMessage(String message, Class<T> eventClass, java.util.function.BiConsumer<T, OrderSaga> processor) {
//        try {
//            T event = objectMapper.readValue(message, eventClass);
//            OrderSaga saga = getSaga(event);
//            processor.accept(event, saga);
//        } catch (Exception e) {
//            log.error("Failed to process {} response: {}", eventClass.getSimpleName(), message, e);
//        }
//    }

    private OrderSaga getSaga(Object event) {
        try {
            Method getSagaId = event.getClass().getMethod("getSagaId");
            UUID sagaId = (UUID) getSagaId.invoke(event);
            return sagaRepository.findById(sagaId).orElseThrow(() -> {
                log.error("Saga not found: {}", sagaId);
                return new RuntimeException("Saga not found: " + sagaId);
            });
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new SagaException(e.getMessage(), e);
        }
    }
}
