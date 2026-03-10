package ru.binarysimple.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.binarysimple.order.client.BillingServiceClient;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.IdempotencyRecord;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;
import ru.binarysimple.order.repository.IdempotencyRepository;
import ru.binarysimple.order.repository.OrderRepository;
import ru.binarysimple.order.saga.OrderSagaManager;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final OrderRepository orderRepository;

//    private final ObjectMapper objectMapper;

    private final OrderSagaManager orderSagaManager;

//    private final BillingServiceClient billingServiceClient;

//    private final ApplicationEventPublisher eventPublisher;

    private final IdempotencyRepository idempotencyRepository;


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
    public OrderResultDto create(OrderDto dto, String idempotencyKey) {
        // 1. Проверка на существование записи идемпотентности
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyRecord> existingRecord =
                    idempotencyRepository.findByIdempotencyKey(idempotencyKey);

            if (existingRecord.isPresent()) {
                IdempotencyRecord record = existingRecord.get();

                // Если запись ещё не истекла — возвращаем сохранённый результат
                if (record.getExpiresAt().isAfter(java.time.LocalDateTime.now())) {
                    return retrieveCachedResult(record.getOrderId());
                }
                // Если запись истекла — удаляем и продолжаем создание
                idempotencyRepository.deleteByIdempotencyKey(idempotencyKey);
            }
        }

        // 2. Создание заказа
        Order order = orderMapper.toEntity(dto);
        order.setStatus(OrderStatus.NEW);

        // Устанавливаем связь между заказом и позициями
        order.getOrderPositions().forEach(position -> position.setOrder(order));

        Order resultOrder = orderRepository.save(order);

        // 3. Начало саги
        orderSagaManager.startNew(orderMapper.toOrderResultDto(order));

        // 4. Сохранение записи идемпотентности (в той же транзакции)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(idempotencyKey);
            record.setOrderId(resultOrder.getId());
            record.setUsername(dto.getUsername());
            record.setCreatedAt(java.time.LocalDateTime.now());
            record.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(IdempotencyRecord.EXPIRY_MINUTES));
            idempotencyRepository.save(record);
        }

        return orderMapper.toOrderResultDto(resultOrder);
    }

    private OrderResultDto retrieveCachedResult(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order with id `%s` not found".formatted(orderId)
                ));
        return orderMapper.toOrderResultDto(order);
    }


}
