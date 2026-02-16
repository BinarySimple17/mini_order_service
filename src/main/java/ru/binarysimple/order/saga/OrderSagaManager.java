package ru.binarysimple.order.saga;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.dto.OrderResultDto;

@Component
public interface OrderSagaManager {

    void startNew(OrderResultDto order);

    @Scheduled(fixedDelay = 300000) // Каждые 5 минут
    @Transactional
    void recoverStuckSagas();
}
