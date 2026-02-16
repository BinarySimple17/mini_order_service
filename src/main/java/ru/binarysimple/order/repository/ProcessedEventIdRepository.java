package ru.binarysimple.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.binarysimple.order.model.ProcessedEventId;

public interface ProcessedEventIdRepository extends JpaRepository<ProcessedEventId, String> {
}
