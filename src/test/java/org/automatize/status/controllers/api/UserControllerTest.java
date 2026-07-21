package org.automatize.status.controllers.api;

import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.User;
import org.automatize.status.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link UserController}. Security filters and method
 * security are disabled ({@code addFilters = false}); focus is request mapping,
 * bean validation (400), JSON contract, {@code @ResponseStatus} exception
 * mapping (404/409), and delegation to the (mocked) service layer.
 */
@WebMvcTest(controllers = UserController.class)
class UserControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private UserService userService;

    /**
     * Builds a minimal active {@link User} fixture for use in stubbed service
     * responses.
     *
     * @param id the identifier to assign to the user
     * @return a populated sample {@link User}
     */
    private User sampleUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setUsername("johndoe");
        u.setEmail("johndoe@example.com");
        u.setStatus("ACTIVE");
        return u;
    }

    /**
     * Provides a valid JSON request body for creating or updating a user.
     *
     * @return a JSON string with the required user fields populated
     */
    private String validUserJson() {
        return "{\"username\":\"johndoe\",\"email\":\"johndoe@example.com\",\"fullName\":\"John Doe\"}";
    }

    /**
     * Verifies that GET {@code /api/users} returns 200 with a paged JSON body
     * whose content is populated from the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getAllUsers_returnsOkPage() throws Exception {
        when(userService.getAllUsers(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleUser(UUID.randomUUID()))));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("johndoe"));
    }

    /**
     * Verifies that GET {@code /api/users/{id}} returns 200 with the user when
     * the service resolves it.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getUserById_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUserById(id)).thenReturn(sampleUser(id));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    /**
     * Verifies that GET {@code /api/users/{id}} returns 404 when the service
     * raises {@link ResourceNotFoundException}.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getUserById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUserById(id))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + id));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies that POST {@code /api/users} with a valid body returns 201 and
     * the created user.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void createUser_valid_returns201() throws Exception {
        when(userService.createUser(any())).thenReturn(sampleUser(UUID.randomUUID()));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(validUserJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    /**
     * Verifies that POST {@code /api/users} with a body missing the required
     * email fails bean validation and returns 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void createUser_missingEmail_returns400() throws Exception {
        String body = "{\"username\":\"johndoe\",\"fullName\":\"John Doe\"}";
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/users} returns 409 when the service raises
     * {@link DuplicateResourceException} for an existing username.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void createUser_duplicate_returns409() throws Exception {
        when(userService.createUser(any()))
                .thenThrow(new DuplicateResourceException("Username already exists: johndoe"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(validUserJson()))
                .andExpect(status().isConflict());
    }

    /**
     * Verifies that PUT {@code /api/users/{id}} with a valid body returns 200
     * and the updated user.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void updateUser_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.updateUser(eq(id), any())).thenReturn(sampleUser(id));

        mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validUserJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    /**
     * Verifies that PUT {@code /api/users/{id}} with a body missing the
     * required username fails bean validation and returns 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void updateUser_missingUsername_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"email\":\"johndoe@example.com\",\"fullName\":\"John Doe\"}";
        mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that DELETE {@code /api/users/{id}} returns 200 with a success
     * message and delegates deletion to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void deleteUser_returns200Message() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).deleteUser(id);
    }

    /**
     * Verifies that POST {@code /api/users/{id}/change-password} with a valid
     * body returns 200 with a success message and delegates to the service.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void changePassword_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"currentPassword\":\"oldpass12\",\"newPassword\":\"newpass12\"}";

        mockMvc.perform(post("/api/users/{id}/change-password", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).changePassword(eq(id), any());
    }

    /**
     * Verifies that POST {@code /api/users/{id}/change-password} with a body
     * missing the new password fails bean validation and returns 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void changePassword_missingNewPassword_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"currentPassword\":\"oldpass12\"}";
        mockMvc.perform(post("/api/users/{id}/change-password", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that POST {@code /api/users/{id}/change-password} returns 409
     * when the service raises {@link BusinessRuleException} because the current
     * password is incorrect.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void changePassword_wrongCurrent_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BusinessRuleException("Current password is incorrect"))
                .when(userService).changePassword(eq(id), any());

        String body = "{\"currentPassword\":\"oldpass12\",\"newPassword\":\"newpass12\"}";
        mockMvc.perform(post("/api/users/{id}/change-password", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    /**
     * Verifies that PATCH {@code /api/users/{id}/enable} returns 200 with the
     * enabled user.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void enableUser_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.enableUser(id)).thenReturn(sampleUser(id));

        mockMvc.perform(patch("/api/users/{id}/enable", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    /**
     * Verifies that PATCH {@code /api/users/{id}/disable} returns 200 with the
     * disabled user.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void disableUser_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.disableUser(id)).thenReturn(sampleUser(id));

        mockMvc.perform(patch("/api/users/{id}/disable", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    /**
     * Verifies that PATCH {@code /api/users/{id}/role} with a valid {@code role}
     * parameter returns 200 with the updated user.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void updateUserRole_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.updateRole(eq(id), eq("ADMIN"))).thenReturn(sampleUser(id));

        mockMvc.perform(patch("/api/users/{id}/role", id).param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    /**
     * Verifies that PATCH {@code /api/users/{id}/role} without the required
     * {@code role} parameter returns 400.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void updateUserRole_missingRoleParam_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/users/{id}/role", id))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that GET {@code /api/users/organization/{organizationId}} returns
     * 200 with a JSON array of the users belonging to the organization.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getUsersByOrganization_returns200List() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(userService.getUsersByOrganization(orgId)).thenReturn(List.of(sampleUser(UUID.randomUUID())));

        mockMvc.perform(get("/api/users/organization/{organizationId}", orgId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("johndoe"));
    }

    /**
     * Verifies that GET {@code /api/users/profile} returns 200 with the profile
     * of the currently authenticated user.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void getCurrentUserProfile_returns200() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(sampleUser(UUID.randomUUID()));

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    /**
     * Verifies that PUT {@code /api/users/profile} with a valid body returns 200
     * with the updated profile of the currently authenticated user.
     *
     * @throws Exception if the mock request cannot be performed
     */
    @Test
    void updateCurrentUserProfile_valid_returns200() throws Exception {
        when(userService.updateCurrentUserProfile(any())).thenReturn(sampleUser(UUID.randomUUID()));

        mockMvc.perform(put("/api/users/profile").contentType(MediaType.APPLICATION_JSON).content(validUserJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }
}
