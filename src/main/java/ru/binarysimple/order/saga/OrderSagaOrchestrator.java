package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.dto.commands.MakePaymentCommand;
import ru.binarysimple.order.dto.commands.PaymentProcessedEvent;
import ru.binarysimple.order.dto.commands.ReserveStockCommand;
import ru.binarysimple.order.dto.commands.StockReservedEvent;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;
import ru.binarysimple.order.saga.steps.MakePaymentStep;
import ru.binarysimple.order.saga.steps.ReserveStockStep;

import static ru.binarysimple.order.model.OrderStatus.PAYMENT_FAILED;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final MakePaymentStep makePaymentStep; // Внедряем шаги
    private final ReserveStockStep reserveStockStep;
    private final OrderSagaRepository sagaRepository;

//    private InitiateDeliveryStep initiateDeliveryStep;

    public OrderResultDto startOrderSaga(Long orderId) {

        log.info("Starting Saga for Order {}", orderId);

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));

        // --- Выполняем шаги ---
        OrderSaga saga = new OrderSaga();
        saga.setOrderId(order.getId());
        saga.setCurrentStep("BILLING");
        saga.setStatus("PROCESSING");
        sagaRepository.save(saga);

        MakePaymentCommand paymentCmd = new MakePaymentCommand(order);

        StepExecutionResult<PaymentProcessedEvent> paymentResult = makePaymentStep.execute(paymentCmd);
        if (!paymentResult.isSuccess()) {
            log.error("Payment step failed for Order {}: {}", order.getId(), paymentResult.getFailureReason());
            try {
                order.setStatus(OrderStatus.valueOf(paymentResult.getFailureReason()));
            } catch (IllegalArgumentException e) {
                order.setStatus(PAYMENT_FAILED);
            }
            orderRepository.save(order);
            // Saga завершена с ошибкой на первом шаге, компенсация не нужна.
            saga.setStatus("FAILED");
            sagaRepository.save(saga);
            return orderMapper.toOrderResultDto(order);
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        saga.setCurrentStep("WAREHOUSE");
        sagaRepository.save(saga);

        ReserveStockCommand stockCmd = new ReserveStockCommand(orderMapper.toOrderResultDto(order));
        StepExecutionResult<StockReservedEvent> stockResult = reserveStockStep.execute(stockCmd);
        if (!stockResult.isSuccess()) {
            log.error("Stock reservation step failed for Order {}: {}", order.getId(), stockResult.getFailureReason());
            order.setStatus(OrderStatus.RESERVATION_FAILED);
            orderRepository.save(order);
            saga.setStatus("COMPENSATING");
            sagaRepository.save(saga);
            // Нужно компенсировать предыдущий шаг (оплату)
            compensateStep(makePaymentStep, paymentCmd);
            saga.setStatus("FAILED");
            sagaRepository.save(saga);
            return orderMapper.toOrderResultDto(order);
        }

        order.setStatus(OrderStatus.PENDING_RESERVATION);
        orderRepository.save(order);
        saga.setCurrentStep("DELIVERY");
        sagaRepository.save(saga);

//        InitiateDeliveryCommand deliveryCmd = new InitiateDeliveryCommand(orderId);
//        StepExecutionResult<DeliveryInitiatedEvent> deliveryResult = initiateDeliveryStep.execute(deliveryCmd);
//        if (!deliveryResult.isSuccess()) {
//            log.error("Delivery initiation step failed for Order {}: {}", orderId, deliveryResult.getFailureReason());
//            order.setStatus(OrderStatus.DELIVERY_FAILED);
//            orderRepository.save(order);
//            saga.setStatus("COMPENSATING");
//            // Нужно компенсировать предыдущие шаги
//            compensateStep(reserveStockStep, stockCmd);
//            compensateStep(makePaymentStep, paymentCmd);
//            saga.setStatus("FAILED");
//            return;
//        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        saga.setStatus("COMPLETED");
        sagaRepository.save(saga);
        log.info("Saga completed successfully for Order {}", order.getId());
        return orderMapper.toOrderResultDto(order);
    }

    private <C> void compensateStep(SagaStep<C, ?> step, C command) {
        log.info("Compensating step: {}", step.getClass().getSimpleName());
        StepExecutionResult<?> compensationResult = step.compensate(command);
        if (compensationResult.isSuccess()) {
            log.info("Compensation successful for step: {}", step.getClass().getSimpleName());
        } else {
            // обработка неудачной компенсации: алярм, повтор...
            log.error("Compensation failed for step {}: {}", step.getClass().getSimpleName(), compensationResult.getFailureReason());
        }
    }
}
