package org.automatize.status.controllers.api;

import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.SchedulerJdbcDatasource;
import org.automatize.status.models.scheduler.DbType;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.security.UserPrincipal;
import org.automatize.status.services.scheduler.SchedulerDatasourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link SchedulerDatasourceApiController}. Focus is on
 * request mapping, bean validation (400), JSON contract, and
 * {@code @ResponseStatus} exception mapping (404). The controller reads the
 * {@code SecurityContext}, so a {@link UserPrincipal} is installed per test.
 */
@WebMvcTest(controllers = SchedulerDatasourceApiController.class)
class SchedulerDatasourceApiControllerTest extends AbstractApiControllerTest {

    private static final String DATASOURCE_NAME = "Primary DB";
    private static final String DATASOURCES_PATH = "/api/scheduler/datasources";
    private static final String DATASOURCE_ID_PATH = "/api/scheduler/datasources/{id}";
    private static final String DATASOURCE_NOT_FOUND_MESSAGE = "Datasource not found";

    @MockitoBean
    private SchedulerDatasourceService datasourceService;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    /**
     * Installs an authenticated ADMIN {@link UserPrincipal} into the {@link SecurityContextHolder}
     * before each test, since the controller reads the current principal from the security context.
     */
    @BeforeEach
    void setUpPrincipal() {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "admin", "admin@test.local", "pw", "ADMIN",
                UUID.randomUUID(), true, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Clears the {@link SecurityContextHolder} after each test to avoid principal leakage between tests.
     */
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds a sample {@link SchedulerJdbcDatasource} fixture for stubbing service calls.
     *
     * @param id the identifier to assign
     * @return a PostgreSQL datasource named "Primary DB" on host "localhost"
     */
    private SchedulerJdbcDatasource sampleDatasource(UUID id) {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setId(id);
        ds.setName(DATASOURCE_NAME);
        ds.setDbType(DbType.POSTGRESQL);
        ds.setHost("localhost");
        return ds;
    }

    /**
     * Provides a minimal valid JSON request body satisfying the create/update bean-validation constraints.
     *
     * @return a JSON string with name, dbType and host populated
     */
    private String validRequestBody() {
        return "{\"name\":\"Primary DB\",\"dbType\":\"POSTGRESQL\",\"host\":\"localhost\"}";
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/datasources
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/datasources returns 200 with the tenant's list of datasources.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void listDatasources_returnsOkList() throws Exception {
        when(datasourceService.list(any())).thenReturn(List.of(sampleDatasource(UUID.randomUUID())));

        mockMvc.perform(get(DATASOURCES_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(DATASOURCE_NAME))
                .andExpect(jsonPath("$[0].dbType").value("POSTGRESQL"));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/datasources/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies GET /api/scheduler/datasources/{id} returns 200 with the datasource when found.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getDatasource_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.get(eq(id), any())).thenReturn(sampleDatasource(id));

        mockMvc.perform(get(DATASOURCE_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("localhost"));
    }

    /**
     * Verifies GET /api/scheduler/datasources/{id} returns 404 when the service throws
     * {@code ResourceNotFoundException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void getDatasource_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.get(eq(id), any()))
                .thenThrow(new ResourceNotFoundException(DATASOURCE_NOT_FOUND_MESSAGE));

        mockMvc.perform(get(DATASOURCE_ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/datasources
    // -------------------------------------------------------------------------

    /**
     * Verifies POST /api/scheduler/datasources with a valid body returns 201 with the created datasource.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createDatasource_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.create(any(), any(), any())).thenReturn(sampleDatasource(id));

        mockMvc.perform(post(DATASOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(DATASOURCE_NAME));
    }

    /**
     * Verifies POST /api/scheduler/datasources returns 400 when the required name is missing
     * (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createDatasource_missingName_returns400() throws Exception {
        mockMvc.perform(post(DATASOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dbType\":\"POSTGRESQL\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies POST /api/scheduler/datasources returns 400 when the required dbType is missing
     * (bean validation failure).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void createDatasource_missingDbType_returns400() throws Exception {
        mockMvc.perform(post(DATASOURCES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Primary DB\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/scheduler/datasources/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies PUT /api/scheduler/datasources/{id} with a valid body returns 200 with the updated datasource.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void updateDatasource_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.update(eq(id), any(), any(), any())).thenReturn(sampleDatasource(id));

        mockMvc.perform(put(DATASOURCE_ID_PATH, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(DATASOURCE_NAME));
    }

    /**
     * Verifies PUT /api/scheduler/datasources/{id} returns 404 when the service throws
     * {@code ResourceNotFoundException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void updateDatasource_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.update(eq(id), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException(DATASOURCE_NOT_FOUND_MESSAGE));

        mockMvc.perform(put(DATASOURCE_ID_PATH, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/scheduler/datasources/{id}
    // -------------------------------------------------------------------------

    /**
     * Verifies DELETE /api/scheduler/datasources/{id} returns 200 with a success message and delegates
     * to the service.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void deleteDatasource_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(DATASOURCE_ID_PATH, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(datasourceService).delete(eq(id), any());
    }

    /**
     * Verifies DELETE /api/scheduler/datasources/{id} returns 404 when the service throws
     * {@code ResourceNotFoundException}.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void deleteDatasource_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException(DATASOURCE_NOT_FOUND_MESSAGE))
                .when(datasourceService).delete(eq(id), any());

        mockMvc.perform(delete(DATASOURCE_ID_PATH, id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/datasources/{id}/test
    // -------------------------------------------------------------------------

    /**
     * Verifies POST /api/scheduler/datasources/{id}/test returns 200 with the connection-test result map
     * (success flag and latency).
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void testConnection_returnsOkResultMap() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.testConnection(eq(id), any()))
                .thenReturn(Map.of("success", true, "latencyMs", 42L));

        mockMvc.perform(post("/api/scheduler/datasources/{id}/test", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.latencyMs").value(42));
    }

    /**
     * Verifies POST /api/scheduler/datasources/{id}/test returns 404 when the service throws
     * {@code ResourceNotFoundException} for an unknown datasource.
     *
     * @throws Exception if the mock request fails
     */
    @Test
    void testConnection_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.testConnection(eq(id), any()))
                .thenThrow(new ResourceNotFoundException(DATASOURCE_NOT_FOUND_MESSAGE));

        mockMvc.perform(post("/api/scheduler/datasources/{id}/test", id))
                .andExpect(status().isNotFound());
    }
}
