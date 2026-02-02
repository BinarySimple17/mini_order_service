package ru.binarysimple.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.binarysimple.order.dto.*;
import ru.binarysimple.order.exception.BillingServiceException;
import ru.binarysimple.order.model.OperationType;
import ru.binarysimple.order.model.Order;

@Component
@Slf4j
public class BillingServiceClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BillingServiceClient(@Value("${endpoints.billing-service:http://test-name:8081}") String baseUrl) {

        log.info("BillingServiceClient baseUrl: {}", baseUrl);
        this.baseUrl = baseUrl;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    private OperationDto executeOperation(OrderResultDto order, OperationType operationType, String operationName) {
        log.info("Initiating {} for order {}", operationName, order.getId());

        OperationRequest operationRequest = new OperationRequest(
                operationType,
                order.getTotalCost(),
                new AccountOperationDto(order.getUsername()),
                order.getId());

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
                        log.warn("Billing service returned 4xx during {}: {}", operationName, errorDto.getMessage());
                        throw new BillingServiceException(errorDto.getMessage(), errorDto);
                    })
                    .body(OperationDto.class);
            log.info("Successfully completed {} for order {}", operationName, order.getId());
            return operation;
        } catch (BillingServiceException billingServiceException) {
            log.error("Failed to {}: {}", operationName, billingServiceException.getMessage());
            throw billingServiceException;
        } catch (Exception e) {
            log.error("Error during {}: {}", operationName, e.getMessage());
            throw new RuntimeException("Failed to call billing-service for " + operationName + ": " + e.getMessage(), e);
        }
    }

    public OperationDto makePayment(OrderResultDto order) {
        return executeOperation(order, OperationType.PAYMENT, "payment");
    }

    public OperationDto cancelPayment(OrderResultDto order) {
        return executeOperation(order, OperationType.REFUND, "canceling payment");
    }
}
