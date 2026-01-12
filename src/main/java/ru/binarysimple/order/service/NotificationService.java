package ru.binarysimple.order.service;

import ru.binarysimple.order.model.Order;

public interface NotificationService {
    void sendNotification(Order order);
}
