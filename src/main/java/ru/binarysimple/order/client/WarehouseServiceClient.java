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
public class WarehouseServiceClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WarehouseServiceClient(@Value("${endpoints.warehouse-service:http://test-name:8081}") String baseUrl) {

        log.info("WarehouseServiceClient baseUrl: {}", baseUrl);
        this.baseUrl = baseUrl;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void reserveProducts(ProductIDDto productIDDto, int quantity) {
        log.info("Not implemented yet: reserveProducts");
    }

    public void cancelReservation(ProductIDDto productIDDto) {
        log.info("Not implemented yet: cancelReservation");
    }

//    private OperationDto executeOperation(Order order, OperationType operationType, String operationName) {
//        log.info("Initiating {} for order {}", operationName, order.getId());
//
//        OperationRequest operationRequest = new OperationRequest(
//                operationType,
//                order.getTotalCost(),
//                new AccountOperationDto(order.getUsername()));
//
//        try {
//            OperationDto operation = restClient
//                    .post()
//                    .uri("/billing/account/operate")
//                    .header("X-Username", order.getUsername())
//                    .body(operationRequest)
//                    .retrieve()
//                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
//                        String responseBody = new String(response.getBody().readAllBytes());
//                        ErrorDto errorDto = objectMapper.readValue(responseBody, ErrorDto.class);
//                        log.warn("Billing service returned 4xx during {}: {}", operationName, errorDto.getMessage());
//                        throw new BillingServiceException(errorDto.getMessage(), errorDto);
//                    })
//                    .body(OperationDto.class);
//            log.info("Successfully completed {} for order {}", operationName, order.getId());
//            return operation;
//        } catch (BillingServiceException billingServiceException) {
//            log.error("Failed to {}: {}", operationName, billingServiceException.getMessage());
//            throw billingServiceException;
//        } catch (Exception e) {
//            log.error("Error during {}: {}", operationName, e.getMessage());
//            throw new RuntimeException("Failed to call billing-service for " + operationName + ": " + e.getMessage(), e);
//        }
//    }

//    public OperationDto reserveFunds(Order order) {
//        return executeOperation(order, OperationType.RESERVE, "fund reservation");
//    }
//
//    public OperationDto confirmPayment(Order order) {
//        return executeOperation(order, OperationType.CONFIRM, "payment confirmation");
//    }
//
//    public OperationDto cancelReservation(Order order) {
//        return executeOperation(order, OperationType.CANCEL_RESERVATION, "reservation cancellation");
//    }
//
//    public OperationDto refundPayment(Order order) {
//        return executeOperation(order, OperationType.REFUND, "refund");
//    }
}
