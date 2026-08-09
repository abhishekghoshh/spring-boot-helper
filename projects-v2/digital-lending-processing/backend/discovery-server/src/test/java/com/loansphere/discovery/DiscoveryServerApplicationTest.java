package com.loansphere.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DiscoveryServerApplicationTest {

    @Test
    void shouldStartDiscoveryServer() {
        // Verifies Spring context loads successfully with Eureka server
    }
}
