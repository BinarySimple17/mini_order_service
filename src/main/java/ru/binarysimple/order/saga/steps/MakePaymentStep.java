package ru.binarysimple.order.saga.steps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.commands.PaymentProcessedEvent;
import ru.binarysimple.order.dto.commands.MakePaymentCommand;
import ru.binarysimple.order.saga.SagaStep;
import ru.binarysimple.order.saga.StepExecutionResult;

@Component
public class MakePaymentStep implements SagaStep<MakePaymentCommand, PaymentProcessedEvent> {

    @Autowired
    private BillingServiceClient billingServiceClient; // Синхронный вызов

    @Override
    public StepExecutionResult<PaymentProcessedEvent> execute(MakePaymentCommand command) {
        OperationDto response = null;
        try {
            // Вызов Billing Service
            // успешно, если вызов не бросил исключение
            response = billingServiceClient.makePayment(command.getOrder());
//            switch (response.getOperationDto().getType()) {
//                case OperationType.DEPOSIT -> {
//                    return StepExecutionResult.success(response);
// //                    return StepExecutionResult.failure("Payment reservation failed with status: " + response.getStatus());
//                }
//            }
        } catch (Exception e) {
            return StepExecutionResult.failure("Exception during payment reservation: " + e.getMessage());
        }
        return StepExecutionResult.success(new PaymentProcessedEvent(response));
    }

    @Override
    public StepExecutionResult<PaymentProcessedEvent> compensate(MakePaymentCommand command) {
        OperationDto response = null;
        try {
            // Отмена резерва
//            CancelPaymentCommand cancelCmd = new CancelPaymentCommand(command.getOrder());
            response = billingServiceClient.cancelPayment(command.getOrder()); // Вызов метода отмены
            //отмена успешна, если вызов не бросил исключение
            return StepExecutionResult.success(new PaymentProcessedEvent(response)); // Или вернуть событие типа CancelledPaymentEvent
        } catch (Exception e) {
            return StepExecutionResult.failure("Exception during payment cancellation: " + e.getMessage());
        }
    }
}