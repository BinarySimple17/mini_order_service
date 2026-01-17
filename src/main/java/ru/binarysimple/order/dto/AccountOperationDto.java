package ru.binarysimple.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;


@Value
public class AccountOperationDto {
    @NotNull
    @NotEmpty
    String username;
}