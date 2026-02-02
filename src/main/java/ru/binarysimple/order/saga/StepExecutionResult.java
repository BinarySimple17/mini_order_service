package ru.binarysimple.order.saga;

import lombok.Getter;
import ru.binarysimple.order.model.OrderStatus;

// DTO для результата выполнения шага
@Getter
public class StepExecutionResult<T> {

    // Getters
    private final boolean success;
    private final String failureReason;
    private final T resultData; //
//    private final OrderStatus status;

    public StepExecutionResult(boolean success, String failureReason, T resultData) {
        this.success = success;
        this.failureReason = failureReason;
        this.resultData = resultData;
//        this.status = orderStatus;
    }

    public static <T> StepExecutionResult<T> success(T data) {
        return new StepExecutionResult<>(true, null, data);
    }

    public static <T> StepExecutionResult<T> failure(String reason) {
        return new StepExecutionResult<>(false, reason, null);
    }

    public static <T> StepExecutionResult<T> waiting() {
        return new StepExecutionResult<>(true, null, null);
    }
}
