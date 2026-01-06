package ru.binarysimple.order;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.service.OrderService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderResource {

    private final OrderService orderService;

    @GetMapping
    public PagedModel<OrderDto> getAll(@ParameterObject Pageable pageable) {
        Page<OrderDto> orderDtos = orderService.getAll(pageable);
        return new PagedModel<>(orderDtos);
    }

    @GetMapping("/{id}")
    public OrderDto getOne(@PathVariable Long id) {
        return orderService.getOne(id);
    }

    @GetMapping("/by-ids")
    public List<OrderDto> getMany(@RequestParam List<Long> ids) {
        return orderService.getMany(ids);
    }

    @PostMapping
    public OrderDto create(@RequestBody @Valid OrderDto dto) {
        return orderService.create(dto);
    }

    @PatchMapping("/{id}")
    public OrderDto patch(@PathVariable Long id, @RequestBody JsonNode patchNode) throws IOException {
        return orderService.patch(id, patchNode);
    }

    @PatchMapping
    public List<Long> patchMany(@RequestParam List<Long> ids, @RequestBody JsonNode patchNode) throws IOException {
        return orderService.patchMany(ids, patchNode);
    }

    @DeleteMapping("/{id}")
    public OrderDto delete(@PathVariable Long id) {
        return orderService.delete(id);
    }

    @DeleteMapping
    public void deleteMany(@RequestParam List<Long> ids) {
        orderService.deleteMany(ids);
    }
}
