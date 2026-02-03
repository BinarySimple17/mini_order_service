package ru.binarysimple.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import ru.binarysimple.order.dto.ErrorDto;

@Getter
public class OutboxEventSaveException extends RuntimeException {

    public OutboxEventSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
