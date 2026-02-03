package ru.binarysimple.order.saga;

//
public interface SagaStep<Command, EventResult> {
    /**
     * Выполняет шаг Saga.
     *
     * @param command Команда для выполнения шага.
     * @return Результат выполнения шага (успешно/неуспешно, событие и т.д.).
     */
    StepExecutionResult<EventResult> execute(Command command);

    /**
     * Компенсирует действие этого шага.
     *
     * @param command Команда для компенсации (может быть та же, что и в execute, или специальная).
     * @return Результат компенсации.
     */
    StepExecutionResult<EventResult> compensate(Command command);

    StepExecutionResult<EventResult> processEvent(EventResult event);
}

