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
import ru.binarysimple.order.repository.OrderRepository;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final OrderRepository orderRepository;

    private final ObjectMapper objectMapper;

    private final BillingServiceClient billingServiceClient;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<OrderResultDto> getAll(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toOrderResultDto);
    }

    @Override
    public OrderResultDto getOne(Long id) {
        Optional<Order> orderOptional = orderRepository.findById(id);
        return orderMapper.toOrderResultDto(orderOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id))));
    }

    @Override
    public List<OrderResultDto> getMany(List<Long> ids) {
        List<Order> orders = orderRepository.findAllById(ids);
        return orders.stream()
                .map(orderMapper::toOrderResultDto)
                .toList();
    }

    // Этот метод оставлен для обратной совместимости
    // В новом коде следует использовать OrderSagaManager для создания заказов
    @Override
    @Deprecated
    public OrderResultDto create(OrderDto dto) {
        Order order = orderMapper.toEntity(dto);

        order.setStatus(OrderStatus.NEW);

        // Устанавливаем связь между заказом и позициями
        order.getOrderPositions().forEach(position -> position.setOrder(order));

        try {
            OperationDto operation = billingServiceClient.makePayment(order);
            order.setStatus(OrderStatus.PAID);
        } catch (BillingServiceException billingServiceException){
            order.setStatus(OrderStatus.INSUFFICIENT_FUNDS);
        } catch (Exception e) {
            order.setStatus(OrderStatus.FAILED);
        }


        Order resultOrder = orderRepository.save(order);

        // публикуем событие - оно будет обработано ПОСЛЕ коммита
        // в этом событии топравка сообщения в кафку
        // только для transactional
        eventPublisher.publishEvent(new OrderCreatedEvent(resultOrder, Class.class.getName()));

        return orderMapper.toOrderResultDto(resultOrder);
    }

    @Override
    public OrderResultDto patch(Long id, JsonNode patchNode) throws IOException {
        Order order = orderRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity with id `%s` not found".formatted(id)));
        
        ObjectReader reader = objectMapper.readerForUpdating(order);
        Order updated = reader.readValue(patchNode);

        Order resultOrder = orderRepository.save(updated);
        return orderMapper.toOrderResultDto(resultOrder);
    }

    @Override
    public List<Long> patchMany(List<Long> ids, JsonNode patchNode) throws IOException {
        Collection<Order> orders = orderRepository.findAllById(ids);

        for (Order order : orders) {
            OrderDto orderDto = orderMapper.toOrderDto(order);
            objectMapper.readerForUpdating(orderDto).readValue(patchNode);
            orderMapper.updateWithNull(orderDto, order);
        }

        List<Order> resultOrders = orderRepository.saveAll(orders);
        return resultOrders.stream()
                .map(Order::getId)
                .toList();
    }

    @Override
    public OrderResultDto delete(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            orderRepository.delete(order);
        }
        return orderMapper.toOrderResultDto(order);
    }

    @Override
    public void deleteMany(List<Long> ids) {
        orderRepository.deleteAllById(ids);
    }
}
