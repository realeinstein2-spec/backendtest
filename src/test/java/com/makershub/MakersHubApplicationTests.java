package com.makershub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires running Docker Desktop/daemon to run Testcontainers")
class MakersHubApplicationTests {

    @Test
    void contextLoads() {
    }
}
