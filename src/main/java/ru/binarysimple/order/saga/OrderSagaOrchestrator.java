package ru.binarysimple.order.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.commands.MakePaymentCommand;
import ru.binarysimple.order.dto.commands.PaymentProcessedEvent;
import ru.binarysimple.order.dto.commands.ReserveStockCommand;
import ru.binarysimple.order.dto.commands.StockReservedEvent;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderSaga;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.steps.MakePaymentStep;
import ru.binarysimple.order.saga.steps.ReserveStockStep;

@Service
@Transactional
@Slf4j
public class OrderSagaOrchestrator {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MakePaymentStep makePaymentStep; // Внедряем шаги

    @Autowired
    private ReserveStockStep reserveStockStep;

//    @Autowired
//    private InitiateDeliveryStep initiateDeliveryStep;


    public void startOrderSaga(Order order) {
        log.info("Starting Saga for Order {}", order.getId());

        // --- Выполняем шаги ---
        OrderSaga saga = new OrderSaga();
        saga.setOrderId(order.getId());
        saga.setStatus("PROCESSING");

        MakePaymentCommand paymentCmd = new MakePaymentCommand(order);

        StepExecutionResult<PaymentProcessedEvent> paymentResult = makePaymentStep.execute(paymentCmd);
        if (!paymentResult.isSuccess()) {
            log.error("Payment step failed for Order {}: {}", order.getId(), paymentResult.getFailureReason());
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            // Saga завершена с ошибкой на первом шаге, компенсация не нужна.
            return;
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        saga.setCurrentStep("WAREHOUSE");

        ReserveStockCommand stockCmd = new ReserveStockCommand(order);
        StepExecutionResult<StockReservedEvent> stockResult = reserveStockStep.execute(stockCmd);
        if (!stockResult.isSuccess()) {
            log.error("Stock reservation step failed for Order {}: {}", order.getId(), stockResult.getFailureReason());
            order.setStatus(OrderStatus.RESERVATION_FAILED);
            orderRepository.save(order);
            saga.setStatus("COMPENSATING");
            // Нужно компенсировать предыдущий шаг (оплату)
            compensateStep(makePaymentStep, paymentCmd);
            saga.setStatus("FAILED");
            return;
        }

        order.setStatus(OrderStatus.PENDING_RESERVATION);
        orderRepository.save(order);
        saga.setCurrentStep("DELIVERY");

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
        log.info("Saga completed successfully for Order {}", order.getId());
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
