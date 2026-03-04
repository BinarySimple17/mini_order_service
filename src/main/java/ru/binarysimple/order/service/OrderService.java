package ru.binarysimple.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;

import java.io.IOException;
import java.util.List;

public interface OrderService {
//    Page<OrderResultDto> getAll(Pageable pageable);

    OrderResultDto getOne(Long id);

    List<OrderResultDto> getMany(List<Long> ids);

    OrderResultDto create(OrderDto dto, String idempotencyKey);

//    OrderResultDto patch(Long id, JsonNode patchNode) throws IOException;

//    List<Long> patchMany(List<Long> ids, JsonNode patchNode) throws IOException;

//    OrderResultDto delete(Long id);

//    void deleteMany(List<Long> ids);
}
