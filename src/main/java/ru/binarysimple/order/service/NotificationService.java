package ru.binarysimple.order.service;

import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.saga.OrderSaga;

public interface NotificationService {
    void sendNotification(OrderSaga saga, OrderResultDto order, EventType eventType);
}
