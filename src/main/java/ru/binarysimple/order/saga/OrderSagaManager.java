package ru.binarysimple.order.saga;

import org.springframework.stereotype.Component;
import ru.binarysimple.order.dto.OrderResultDto;

@Component
public interface OrderSagaManager {

    void startNew(OrderResultDto order);

//    void recover();
}
