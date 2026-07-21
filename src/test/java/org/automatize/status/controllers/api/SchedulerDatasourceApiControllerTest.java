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

    @MockitoBean
    private SchedulerDatasourceService datasourceService;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setUpPrincipal() {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "admin", "admin@test.local", "pw", "ADMIN",
                UUID.randomUUID(), true, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private SchedulerJdbcDatasource sampleDatasource(UUID id) {
        SchedulerJdbcDatasource ds = new SchedulerJdbcDatasource();
        ds.setId(id);
        ds.setName("Primary DB");
        ds.setDbType(DbType.POSTGRESQL);
        ds.setHost("localhost");
        return ds;
    }

    private String validRequestBody() {
        return "{\"name\":\"Primary DB\",\"dbType\":\"POSTGRESQL\",\"host\":\"localhost\"}";
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/datasources
    // -------------------------------------------------------------------------

    @Test
    void listDatasources_returnsOkList() throws Exception {
        when(datasourceService.list(any())).thenReturn(List.of(sampleDatasource(UUID.randomUUID())));

        mockMvc.perform(get("/api/scheduler/datasources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Primary DB"))
                .andExpect(jsonPath("$[0].dbType").value("POSTGRESQL"));
    }

    // -------------------------------------------------------------------------
    // GET /api/scheduler/datasources/{id}
    // -------------------------------------------------------------------------

    @Test
    void getDatasource_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.get(eq(id), any())).thenReturn(sampleDatasource(id));

        mockMvc.perform(get("/api/scheduler/datasources/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("localhost"));
    }

    @Test
    void getDatasource_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.get(eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Datasource not found"));

        mockMvc.perform(get("/api/scheduler/datasources/{id}", id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/datasources
    // -------------------------------------------------------------------------

    @Test
    void createDatasource_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.create(any(), any(), any())).thenReturn(sampleDatasource(id));

        mockMvc.perform(post("/api/scheduler/datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Primary DB"));
    }

    @Test
    void createDatasource_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/scheduler/datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dbType\":\"POSTGRESQL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDatasource_missingDbType_returns400() throws Exception {
        mockMvc.perform(post("/api/scheduler/datasources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Primary DB\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/scheduler/datasources/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateDatasource_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.update(eq(id), any(), any(), any())).thenReturn(sampleDatasource(id));

        mockMvc.perform(put("/api/scheduler/datasources/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Primary DB"));
    }

    @Test
    void updateDatasource_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.update(eq(id), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Datasource not found"));

        mockMvc.perform(put("/api/scheduler/datasources/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/scheduler/datasources/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteDatasource_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/scheduler/datasources/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(datasourceService).delete(eq(id), any());
    }

    @Test
    void deleteDatasource_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Datasource not found"))
                .when(datasourceService).delete(eq(id), any());

        mockMvc.perform(delete("/api/scheduler/datasources/{id}", id))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/scheduler/datasources/{id}/test
    // -------------------------------------------------------------------------

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

    @Test
    void testConnection_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(datasourceService.testConnection(eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Datasource not found"));

        mockMvc.perform(post("/api/scheduler/datasources/{id}/test", id))
                .andExpect(status().isNotFound());
    }
}
