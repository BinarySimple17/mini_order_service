package ru.binarysimple.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.binarysimple.order.dto.AccountOperationDto;
import ru.binarysimple.order.dto.ErrorDto;
import ru.binarysimple.order.dto.OperationDto;
import ru.binarysimple.order.dto.OperationRequest;
import ru.binarysimple.order.exception.BillingServiceException;
import ru.binarysimple.order.model.OperationType;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;

import java.net.URI;

@Component
@Slf4j
public class BillingServiceClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BillingServiceClient(@Value("${endpoints.billing-service:http://test-name:8081}") String baseUrl) {

        log.info("UsersServiceClient baseUrl: {}", baseUrl);
        this.baseUrl = baseUrl;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public OperationDto makePayment(Order order) {
        log.info("Initiating payment for order {}", order.getId());
    }

    public OperationDto reserveFunds(Order order) {
        log.info("Reserving funds for order {}", order.getId());

        OperationRequest operationRequest = new OperationRequest(
                OperationType.RESERVE,
                order.getTotalCost(),
                new AccountOperationDto(order.getUsername()));

        try {
            OperationDto operation = restClient
                    .post()
                    .uri("/billing/account/operate")
                    .header("X-Username", order.getUsername())
                    .body(operationRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String responseBody = new String(response.getBody().readAllBytes());
                        ErrorDto errorDto = objectMapper.readValue(responseBody, ErrorDto.class);
                        log.warn("Billing service returned 4xx during fund reservation: {}", errorDto.getMessage());
                        throw new BillingServiceException(errorDto.getMessage(), errorDto);
                    })
                    .body(OperationDto.class);
            log.info("Successfully reserved funds for order {}", order.getId());
            return operation;
        } catch (BillingServiceException billingServiceException) {
            log.error("Failed to reserve funds for order {}: {}", order.getId(), billingServiceException.getMessage());
            throw billingServiceException;
        } catch (Exception e) {
            log.error("Failed to reserve funds for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Failed to call billing-service for fund reservation: " + e.getMessage(), e);
        }
    }

    public OperationDto confirmPayment(Order order) {
        log.info("Confirming payment for order {}", order.getId());

        OperationRequest operationRequest = new OperationRequest(
                OperationType.CONFIRM,
                order.getTotalCost(),
                new AccountOperationDto(order.getUsername()));

        try {
            OperationDto operation = restClient
                    .post()
                    .uri("/billing/account/operate")
                    .header("X-Username", order.getUsername())
                    .body(operationRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String responseBody = new String(response.getBody().readAllBytes());
                        ErrorDto errorDto = objectMapper.readValue(responseBody, ErrorDto.class);
                        log.warn("Billing service returned 4xx during payment confirmation: {}", errorDto.getMessage());
                        throw new BillingServiceException(errorDto.getMessage(), errorDto);
                    })
                    .body(OperationDto.class);
            log.info("Successfully confirmed payment for order {}", order.getId());
            return operation;
        } catch (BillingServiceException billingServiceException) {
            log.error("Failed to confirm payment for order {}: {}", order.getId(), billingServiceException.getMessage());
            throw billingServiceException;
        } catch (Exception e) {
            log.error("Failed to confirm payment for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Failed to call billing-service for payment confirmation: " + e.getMessage(), e);
        }
    }

    public OperationDto cancelReservation(Order order) {
        log.info("Canceling reservation for order {}", order.getId());

        OperationRequest operationRequest = new OperationRequest(
                OperationType.CANCEL_RESERVATION,
                order.getTotalCost(),
                new AccountOperationDto(order.getUsername()));

        try {
            OperationDto operation = restClient
                    .post()
                    .uri("/billing/account/operate")
                    .header("X-Username", order.getUsername())
                    .body(operationRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String responseBody = new String(response.getBody().readAllBytes());
                        ErrorDto errorDto = objectMapper.readValue(responseBody, ErrorDto.class);
                        log.warn("Billing service returned 4xx during reservation cancellation: {}", errorDto.getMessage());
                        throw new BillingServiceException(errorDto.getMessage(), errorDto);
                    })
                    .body(OperationDto.class);
            log.info("Successfully canceled reservation for order {}", order.getId());
            return operation;
        } catch (BillingServiceException billingServiceException) {
            log.error("Failed to cancel reservation for order {}: {}", order.getId(), billingServiceException.getMessage());
            throw billingServiceException;
        } catch (Exception e) {
            log.error("Failed to cancel reservation for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Failed to call billing-service for reservation cancellation: " + e.getMessage(), e);
        }
    }

    public OperationDto refundPayment(Order order) {
        log.info("Refunding payment for order {}", order.getId());

        OperationRequest operationRequest = new OperationRequest(
                OperationType.REFUND,
                order.getTotalCost(),
                new AccountOperationDto(order.getUsername()));

        try {
            OperationDto operation = restClient
                    .post()
                    .uri("/billing/account/operate")
                    .header("X-Username", order.getUsername())
                    .body(operationRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String responseBody = new String(response.getBody().readAllBytes());
                        ErrorDto errorDto = objectMapper.readValue(responseBody, ErrorDto.class);
                        log.warn("Billing service returned 4xx during refund: {}", errorDto.getMessage());
                        throw new BillingServiceException(errorDto.getMessage(), errorDto);
                    })
                    .body(OperationDto.class);
            log.info("Successfully refunded payment for order {}", order.getId());
            return operation;
        } catch (BillingServiceException billingServiceException) {
            log.error("Failed to refund payment for order {}: {}", order.getId(), billingServiceException.getMessage());
            throw billingServiceException;
        } catch (Exception e) {
            log.error("Failed to refund payment for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Failed to call billing-service for refund: " + e.getMessage(), e);
        }
    }
        log.info("Initiating payment for order {}", order.getId());
        log.info("Initiating payment for order {}", order.getId());
        log.info("Calling billing-service to make payment for user: {}", order.getUsername());

        OperationRequest operationRequest = new OperationRequest(
                OperationType.PAYMENT,
                order.getTotalCost(),
                new AccountOperationDto(order.getUsername()));

        String uriTemplate = "/billing/account/operate";
        // Построим финальный URI
        URI finalUri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path(uriTemplate)
                .buildAndExpand(order.getUsername())
                .encode()
                .toUri();
        log.info("Calling billing-service: POST {}", finalUri);
        try {
            OperationDto operation = restClient
                    .post()
                    .uri("/billing/account/operate")
                    .header("X-Username", order.getUsername())
                    .body(operationRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        // Читаем тело ошибки как ErrorDto
                        String responseBody = new String(response.getBody().readAllBytes());
                        ErrorDto errorDto = objectMapper.readValue(responseBody, ErrorDto.class);
                        log.warn("Billing service returned 4xx: {}", errorDto.getMessage());
                        throw new BillingServiceException(errorDto.getMessage(), errorDto);
                    })
                    .body(OperationDto.class);
            log.info("Successfully made payment for: {}", order.getUsername());
            return operation;
        } catch (BillingServiceException billingServiceException) {
            throw billingServiceException;
        } catch (Exception e) {
            log.error("Failed to make payment for {}: {}", order.getUsername(), e.getMessage());
            throw new RuntimeException("Failed to call billing-service: " + e.getMessage(), e);
        }
    }
}
