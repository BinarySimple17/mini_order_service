package ru.binarysimple.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.binarysimple.order.dto.UserDto;

import java.net.URI;

@Component
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;
    private final String baseUrl;

    @Autowired
    public UserServiceClient(@Value("${endpoints.users-service:http://test-name:8081}") String baseUrl) {

        log.info("UsersServiceClient baseUrl: {}", baseUrl);
        this.baseUrl = baseUrl;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public UserDto getUser(String username) {
        log.info("Calling user-service to get  my profile for user: {}", username);

        String uriTemplate = "/user?username={username}";
        // Построим финальный URI
        URI finalUri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path(uriTemplate)
                .buildAndExpand(username)
                .encode()
                .toUri();
        log.info("Calling user-service: GET {}", finalUri);

        try {
            UserDto user = restClient
                    .get()
                    .uri("/user?username={username}", username)
                    .header("X-Username", username)
                    .retrieve()
                    .body(UserDto.class);
            log.info("Successfully get user profile for: {}", username);
            return user;
        } catch (Exception e) {
            log.error("Failed to get user profile for {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to call user-service: " + e.getMessage(), e);
        }
    }

}
