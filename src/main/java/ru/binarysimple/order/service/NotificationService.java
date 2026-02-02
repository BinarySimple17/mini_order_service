package ru.binarysimple.order.service;

import ru.binarysimple.order.dto.OrderResultDto;
import ru.binarysimple.order.model.Order;

public interface NotificationService {
    void sendNotification(OrderResultDto order);
}
