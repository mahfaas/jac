package com.shuld.jac.orderservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.shuld.jac.orderservice.client.UserServiceClient;
import com.shuld.jac.orderservice.dto.UserInfoDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceClientCircuitBreakerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserServiceClient userServiceClient;

    @Test
    void circuitOpensAfterThresholdFailures_andShortCircuitsWithoutHittingWireMock() {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/users/internal/1"))
                .willReturn(WireMock.aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        for (int i = 0; i < 5; i++) {
            UserInfoDto result = userServiceClient.getUserById(1L);
            assertThat(result).isNull();
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        wireMock.resetRequests();

        UserInfoDto resultWhileOpen = userServiceClient.getUserById(1L);

        assertThat(resultWhileOpen).isNull();
        wireMock.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/users/internal/1")));
    }
}
