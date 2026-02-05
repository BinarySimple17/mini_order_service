package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.repository.OrderSagaRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaRecoveryService {

    private final OrderSagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaCompensator sagaCompensator;
}
