package ru.binarysimple.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import ru.binarysimple.order.model.OperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationDto {
    Long id;
    LocalDateTime createdAt;
    OperationType type;
    BigDecimal amount;
}