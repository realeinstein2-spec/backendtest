package com.makershub.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "MONOLITH_URL=http://localhost:9999"
})
public class GatewayRoutingTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void testHealthEndpoint() {
        webClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testProtectedRouteWithoutAuthReturns401() {
        webClient.get().uri("/api/v1/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void testPublicRouteWithoutAuthBypassesFilter() {
        webClient.get().uri("/api/v1/auth/login")
                .exchange()
                // Since downstream is unreachable, Gateway returns 500 or 503 instead of 401
                .expectStatus().is5xxServerError();
    }
}
