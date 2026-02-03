package ru.binarysimple.order.saga.processor;

import lombok.Getter;

@Getter
public class ProcessorResult<T> {

    private final boolean success;
    private final String failureReason;
    private final T resultData; //


    public ProcessorResult(boolean success, String failureReason, T resultData) {
        this.success = success;
        this.failureReason = failureReason;
        this.resultData = resultData;
    }

    public static <T> ProcessorResult<T> success(T data) {
        return new ProcessorResult<>(true, null, data);
    }

    public static <T> ProcessorResult<T> failure(String reason) {
        return new ProcessorResult<>(false, reason, null);
    }

    public static <T> ProcessorResult<T> waiting() {
        return new ProcessorResult<>(true, null, null);
    }
}
