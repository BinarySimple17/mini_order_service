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
