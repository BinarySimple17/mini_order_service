package ru.binarysimple.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import ru.binarysimple.order.dto.ErrorDto;

@Getter
public class BillingServiceException extends RuntimeException {
    private final ErrorDto errorDto;

    public BillingServiceException(String message) {
        super(message);
        this.errorDto = new ErrorDto(HttpStatus.BAD_REQUEST.value(), message);
    }

    public BillingServiceException(String message, ErrorDto errorDto) {
        super(message);
        this.errorDto = errorDto;
    }

}
