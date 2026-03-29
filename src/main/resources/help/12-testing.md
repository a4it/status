# Section 12: Testing

---

## 157. Test Strategy Overview

The project targets two test types:

| Type | Scope | Tools | What is tested |
|------|-------|-------|----------------|
| **Unit tests** | Individual service/utility methods | JUnit 5, Mockito | Business logic in isolation; no DB, no Spring context |
| **Integration tests** | Full request/response cycle | `@SpringBootTest`, `TestRestTemplate` | Controller → Service → Repository → DB |

**What is explicitly NOT mocked:**
- The database (integration tests use a real test PostgreSQL or H2 in-memory DB)
- Spring Security filter chain (integration tests authenticate with real JWT tokens)

**What IS mocked in unit tests:**
- Repository interfaces (return stubbed entities)
- External services (`EmailService`, `HealthCheckService` network calls)

---

## 158. Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=StatusApplicationTests

# Run with a specific profile (activates application-test.properties)
mvn test -Dspring.profiles.active=test

# Run and generate Surefire HTML report
mvn test surefire-report:report
# Report at: target/site/surefire-report.html
```

**Parallel test execution:** Tests can run in parallel by adding `forkCount` to the Maven Surefire plugin configuration. Be careful with integration tests that share a database — use `@Transactional` with rollback to prevent state leakage.

---

## 159. Test Database Configuration

Create `src/test/resources/application-test.properties`:

```properties
# Use a separate test database
spring.datasource.url=jdbc:postgresql://localhost:5432/uptime_test
spring.datasource.username=postgres
spring.datasource.password=your-test-password

# Or use H2 in-memory (requires H2 dependency and Flyway H2 compatibility)
# spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
# spring.datasource.driver-class-name=org.h2.Driver
# spring.jpa.hibernate.ddl-auto=create-drop

# Run Flyway migrations on test DB
spring.flyway.locations=classpath:db/migration,classpath:db/test-data
spring.flyway.clean-on-validation-error=true

# Disable email in tests
app.email.enabled=false

# Disable health check scheduler in tests
health-check.enabled=false

# Disable data initializer (use test fixtures instead)
data.initializer.enabled=false
```

**Test data:** Create `src/test/resources/db/test-data/` for test-specific seed migrations (e.g., `V901__test_seed_data.sql`).

---

## 160. Writing Service Unit Tests

**Pattern:** Mock the repository, test the service method.

```java
@ExtendWith(MockitoExtension.class)
class StatusPlatformServiceTest {

    @Mock
    private StatusPlatformRepository platformRepository;

    @InjectMocks
    private StatusPlatformService platformService;

    @Test
    void createPlatform_shouldSaveAndReturn() {
        // Arrange
        StatusPlatformRequest request = new StatusPlatformRequest();
        request.setName("My Platform");

        StatusPlatform saved = new StatusPlatform();
        saved.setId(UUID.randomUUID());
        saved.setName("My Platform");

        when(platformRepository.save(any())).thenReturn(saved);

        // Act
        StatusPlatform result = platformService.create(request, mockUserPrincipal());

        // Assert
        assertNotNull(result.getId());
        assertEquals("My Platform", result.getName());
        verify(platformRepository, times(1)).save(any());
    }
}
```

**Assertion conventions:**
- Use `assertEquals`, `assertNotNull`, `assertThrows` from JUnit 5
- Use `verify(mock, times(n)).method(...)` for interaction verification
- Use `ArgumentCaptor` when you need to inspect the exact argument passed to a mock

---

## 161. Writing Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class StatusPlatformControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Get a real JWT token for the test admin user
        LoginRequest loginRequest = new LoginRequest("admin", "admin");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/auth/login", loginRequest, AuthResponse.class);
        authToken = response.getBody().getAccessToken();
    }

    @Test
    void createPlatform_asAdmin_shouldReturn201() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        StatusPlatformRequest request = new StatusPlatformRequest();
        request.setName("Integration Test Platform");

        HttpEntity<StatusPlatformRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<StatusPlatform> response = restTemplate.postForEntity(
            "/api/status-platforms", entity, StatusPlatform.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }
}
```

**`@Transactional` on test class:** Rolls back all database changes after each test method. Ensures tests are isolated and do not affect each other's state.

---

## 162. Security Test Patterns

**Using `@WithMockUser` (unit/slice tests):**

```java
@WebMvcTest(StatusPlatformController.class)
class StatusPlatformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatusPlatformService platformService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getPlatforms_asAdmin_shouldReturn200() throws Exception {
        when(platformService.findAll(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/status-platforms"))
            .andExpect(status().isOk());
    }

    @Test
    void getPlatforms_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/status-platforms"))
            .andExpect(status().isUnauthorized());
    }
}
```

**Using a real JWT in integration tests:** See item 161 — obtain a real token via the login endpoint and include it in the `Authorization` header.

**Testing role-based access:**
```java
@Test
@WithMockUser(username = "user", roles = {"USER"})
void deletePlatform_asUser_shouldReturn403() throws Exception {
    mockMvc.perform(delete("/api/status-platforms/" + testId))
        .andExpect(status().isForbidden());
}
```

---

## 163. Test Coverage Targets

| Package | Minimum coverage target |
|---------|------------------------|
| `services/` | 80% line coverage |
| `security/` | 90% line coverage |
| `controllers/api/` | 70% line coverage (integration tests count) |
| `models/` | Not tested directly (tested via service tests) |
| `repositories/` | Not tested directly (Spring Data auto-implements) |

**Measuring coverage:**
```bash
mvn test jacoco:report
# Report at: target/site/jacoco/index.html
```

Add JaCoCo to `pom.xml` if not already present:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
