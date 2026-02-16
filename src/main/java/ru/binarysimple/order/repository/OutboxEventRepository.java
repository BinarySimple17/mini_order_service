package ru.binarysimple.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.binarysimple.order.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT e FROM OutboxEvent e " + "WHERE e.published = false "
            + "AND e.retryCount < :maxRetries "
            + "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents(@Param("maxRetries") int maxRetries);

    @Modifying
    @Query("UPDATE OutboxEvent e " + "SET e.retryCount = e.retryCount + 1, "
            + "e.errorMessage = :error "
            + "WHERE e.eventId = :eventId")
    int incrementRetryCount(@Param("eventId") String eventId, @Param("error") String error);
//
//    @Modifying
//    @Query("UPDATE OutboxEvent e " + "SET e.published = true, "
//            + "e.publishedAt = :publishedAt, "
//            + "e.errorMessage = null "
//            + "WHERE e.eventId = :eventId")
//    int markAsPublished(@Param("eventId") String eventId, @Param("publishedAt") LocalDateTime publishedAt);
}
