package com.shuld.jac.orderservice.client;

import com.shuld.jac.orderservice.dto.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient restClient;

    public UserServiceClient(RestClient.Builder restClientBuilder,
                              @Value("${user-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserInfoDto getUserById(Long id) {
        return restClient.get()
                .uri("/api/users/internal/{id}", id)
                .retrieve()
                .body(UserInfoDto.class);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserInfoDto getUserByEmail(String email) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/users/internal/by-email")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .body(UserInfoDto.class);
    }

    private UserInfoDto getUserByIdFallback(Long id, Throwable throwable) {
        log.warn("User Service unavailable while fetching user id={}: {}", id, throwable.getMessage());
        return null;
    }

    private UserInfoDto getUserByEmailFallback(String email, Throwable throwable) {
        log.warn("User Service unavailable while fetching user email={}: {}", email, throwable.getMessage());
        return null;
    }
}
