package ru.binarysimple.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.binarysimple.order.model.saga.OrderSaga;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {

    List<OrderSaga> findByStateIn(Collection<OrderSaga.SagaState> states);

    @Query("SELECT s FROM OrderSaga s " + "WHERE s.state IN :states ")
    List<OrderSaga> findStuckSagas(@Param("states") List<OrderSaga.SagaState> states);

}