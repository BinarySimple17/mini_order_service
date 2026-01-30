package ru.binarysimple.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.saga.OrderSagaOrchestrator;
import ru.binarysimple.order.service.OrderService;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderSagaOrchestrator orderSagaOrchestrator;
//    private final OrderSagaManager orderSagaManager;

//    public OrderController(OrderService orderService, OrderSagaManager orderSagaManager) {
//        this.orderService = orderService;
//        this.orderSagaManager = orderSagaManager;
//    }


    @GetMapping("/{id}")
    public OrderResultDto getOne(@PathVariable Long id) {
        return orderService.getOne(id);
    }

    @PostMapping
    public OrderResultDto create(@RequestHeader("X-Username") String currentUsername, @RequestBody @Valid OrderDto dto) {
        if (!currentUsername.equals(dto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return orderService.create(dto);
    }

//    @DeleteMapping("/{id}")
//    public OrderResultDto cancelOrder(@PathVariable Long id) {
////        orderSagaOrchestrator.cancelOrder(id);
//        return orderService.getOne(id);
//    }

//    @GetMapping
//    public PagedModel<OrderDto> getAll(@ParameterObject Pageable pageable) {
//        Page<OrderDto> orderDtos = orderService.getAll(pageable);
//        return new PagedModel<>(orderDtos);
//    }

//    @GetMapping("/by-ids")
//    public List<OrderDto> getMany(@RequestParam List<Long> ids) {
//        return orderService.getMany(ids);
//    }

//    @PatchMapping("/{id}")
//    public OrderDto patch(@PathVariable Long id, @RequestBody JsonNode patchNode) throws IOException {
//        return orderService.patch(id, patchNode);
//    }

//    @PatchMapping
//    public List<Long> patchMany(@RequestParam List<Long> ids, @RequestBody JsonNode patchNode) throws IOException {
//        return orderService.patchMany(ids, patchNode);
//    }

    // Delete endpoints заменены на cancelOrder
    // Полное удаление заказов запрещено для сохранения истории
    // @DeleteMapping("/{id}")
    // public OrderDto delete(@PathVariable Long id) {
    //     return orderService.delete(id);
    // }

    // @DeleteMapping
    // public void deleteMany(@RequestParam List<Long> ids) {
    //     orderService.deleteMany(ids);
    // }
}
