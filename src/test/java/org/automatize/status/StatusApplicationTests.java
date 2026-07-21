package org.automatize.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test for the {@link StatusApplication} Spring Boot application.
 *
 * <p>Boots the full application context under the {@code test} profile to verify
 * that the wiring of beans, configuration and component scanning is valid.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class StatusApplicationTests {

    /**
     * Verifies that the Spring application context starts up successfully.
     * The test passes as long as context initialization completes without throwing.
     */
    @Test
    void contextLoads() {
    }

}
