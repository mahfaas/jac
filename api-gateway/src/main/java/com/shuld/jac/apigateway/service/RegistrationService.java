package com.shuld.jac.apigateway.service;

import com.shuld.jac.apigateway.dto.CredentialPayload;
import com.shuld.jac.apigateway.dto.CredentialResponse;
import com.shuld.jac.apigateway.dto.RegisterRequest;
import com.shuld.jac.apigateway.dto.UserPayload;
import com.shuld.jac.apigateway.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final WebClient userServiceClient;
    private final WebClient authServiceClient;

    public RegistrationService(WebClient.Builder webClientBuilder,
                                @Value("${user-service.base-url}") String userServiceBaseUrl,
                                @Value("${auth-service.base-url}") String authServiceBaseUrl) {
        this.userServiceClient = webClientBuilder.clone().baseUrl(userServiceBaseUrl).build();
        this.authServiceClient = webClientBuilder.clone().baseUrl(authServiceBaseUrl).build();
    }

    public Mono<CredentialResponse> register(RegisterRequest request) {
        UserPayload userPayload = new UserPayload(request.getName(), request.getSurname(),
                request.getBirthDate(), request.getEmail());

        return userServiceClient.post()
                .uri("/api/users")
                .bodyValue(userPayload)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .flatMap(createdUser -> registerCredentials(createdUser, request));
    }

    private Mono<CredentialResponse> registerCredentials(UserResponse createdUser, RegisterRequest request) {
        CredentialPayload credentialPayload = new CredentialPayload(createdUser.getId(),
                request.getLogin(), request.getPassword(), request.getRole());

        return authServiceClient.post()
                .uri("/api/auth/register")
                .bodyValue(credentialPayload)
                .retrieve()
                .bodyToMono(CredentialResponse.class)
                .onErrorResume(authError -> rollbackUser(createdUser.getId())
                        .onErrorResume(rollbackError -> {
                            log.error("Failed to roll back user id={} after auth-service registration failure",
                                    createdUser.getId(), rollbackError);
                            return Mono.empty();
                        })
                        .then(Mono.error(authError)));
    }

    private Mono<Void> rollbackUser(Long userId) {
        return userServiceClient.delete()
                .uri("/api/users/internal/{id}", userId)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}
