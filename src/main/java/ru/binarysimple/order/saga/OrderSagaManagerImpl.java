package ru.binarysimple.order.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.client.DeliveryServiceClient;
import ru.binarysimple.order.client.WarehouseServiceClient;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.steps.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@RequiredArgsConstructor
@Slf4j
@Component
public class OrderSagaManagerImpl implements OrderSagaManager {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final BillingServiceClient billingServiceClient;
    private final DeliveryServiceClient deliveryServiceClient;
    private final WarehouseServiceClient warehouseServiceClient;

    private final List<SagaStep> sagaSteps = new ArrayList<>();

    @Override
    public void addStep(SagaStep step) {
        sagaSteps.add(step);
    }

    @Override
    public void execute() {
        Stack<SagaStep> executedSteps = new Stack<>();
        try {
            for (SagaStep step : sagaSteps) {
                step.perform();
                executedSteps.push(step);
            }
        } catch (Exception e) {
            log.error("Saga execution failed, starting compensation", e);
            while (!executedSteps.isEmpty()) {
                SagaStep step = executedSteps.pop();
                try {
                    step.compensate();
                } catch (Exception compensateException) {
                    log.error("Compensation failed for step: {}", step.getClass().getSimpleName(), compensateException);
                    // Continue compensating other steps
                }
            }
            // Re-throw original exception after compensation
            throw new RuntimeException("Saga execution failed: " + e.getMessage(), e);
        } finally {
            sagaSteps.clear();
        }
    }

    @Override
    @Transactional
    public OrderResultDto createOrder(OrderDto orderDto) {
        log.info("Starting order creation saga for order");
        
        try {
            // Создаем шаги саги
            CreateOrderStep createOrderStep = new CreateOrderStep(orderDto, orderRepository, orderMapper);
            addStep(createOrderStep);
            
            addStep(new MakePaymentStep(createOrderStep.getSavedOrder(), billingServiceClient, orderRepository));
            
            addStep(new ReserveProductWarehouseStep(createOrderStep.getSavedOrder(), warehouseServiceClient, orderRepository));
            
             addStep(new CreateDeliveryStep(createOrderStep.getSavedOrder(), deliveryServiceClient, orderRepository));
            
            // Выполняем сагу
            execute();
            
            log.info("Order saga completed successfully");
            return orderMapper.toOrderResultDto(createOrderStep.getSavedOrder());
            
        } catch (Exception e) {
            log.error("Order saga failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void cancelOrder(Long orderId) {
        log.info("Not yet implemented: cancelOrder step");
    }

    // Метод cancelOrder больше не нужен, так как отмена обрабатывается через компенсирующие шаги
    // При сбое в любом шаге саги автоматически запускаются соответствующие компенсирующие действия
    // через механизм execute() и стек выполненных шагов.
    // Все компенсирующие действия теперь обрабатываются на уровне отдельных шагов саги
    // через метод compensate() каждого SagaStep
}
