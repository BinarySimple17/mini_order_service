package ru.binarysimple.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import ru.binarysimple.order.model.OperationType;

import java.math.BigDecimal;

@Value
public class OperationRequest {
    OperationType type;
    BigDecimal amount;
    @NotNull
    AccountOperationDto account;
}