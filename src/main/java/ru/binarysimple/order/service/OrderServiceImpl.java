package ru.binarysimple.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.event.OrderCreatedEvent;
import ru.binarysimple.order.exception.BillingServiceException;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderPosition;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.model.saga.OrderSaga;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.OrderSagaManager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final OrderRepository orderRepository;

    private final ObjectMapper objectMapper;

    private final OrderSagaManager orderSagaManager;

    private final BillingServiceClient billingServiceClient;

    private final ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional(readOnly = true)
    public OrderResultDto getOne(Long id) {
        Optional<Order> orderOptional = orderRepository.findById(id);
        return orderMapper.toOrderResultDto(orderOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResultDto> getMany(List<Long> ids) {
        List<Order> orders = orderRepository.findAllById(ids);
        return orders.stream()
                .map(orderMapper::toOrderResultDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderResultDto create(OrderDto dto) {
        Order order = orderMapper.toEntity(dto);

        order.setStatus(OrderStatus.NEW);

        // Устанавливаем связь между заказом и позициями
        order.getOrderPositions().forEach(position -> position.setOrder(order));

        Order resultOrder = orderRepository.save(order);

        orderSagaManager.startNew(orderMapper.toOrderResultDto(order));

        return orderMapper.toOrderResultDto(resultOrder);
    }


}
