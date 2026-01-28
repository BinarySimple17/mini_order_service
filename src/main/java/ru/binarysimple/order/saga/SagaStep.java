package ru.binarysimple.order.saga;

/**
 * Интерфейс для шага в паттерне Saga.
 * Каждый шаг должен реализовывать методы выполнения и компенсации.
 */
public interface SagaStep {
    
    /**
     * Выполняет основное действие шага.
     * @throws Exception если выполнение шага завершилось с ошибкой
     */
    void perform() throws Exception;
    
    /**
     * Выполняет компенсирующее действие шага.
     * @throws Exception если компенсация шага завершилась с ошибкой
     */
    void compensate() throws Exception;
}