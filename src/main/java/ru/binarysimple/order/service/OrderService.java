package ru.binarysimple.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.binarysimple.order.dto.OrderDto;

import java.io.IOException;
import java.util.List;

public interface OrderService {
    Page<OrderDto> getAll(Pageable pageable);

    OrderDto getOne(Long id);

    List<OrderDto> getMany(List<Long> ids);

    OrderDto create(OrderDto dto);

    OrderDto patch(Long id, JsonNode patchNode) throws IOException;

    List<Long> patchMany(List<Long> ids, JsonNode patchNode) throws IOException;

    OrderDto delete(Long id);

    void deleteMany(List<Long> ids);
}
