package ru.binarysimple.order.service;

import ru.binarysimple.order.model.EventType;
import ru.binarysimple.order.model.ParentType;

public interface OutboxService {

    void saveEvent(EventType eventType, String parentId, ParentType parentType, Object payload, String topic);

    void processOutbox();
}
