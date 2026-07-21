package org.automatize.status.controllers.api;

import org.automatize.status.api.response.NotificationSubscriberResponse;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.services.NotificationSubscriberService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc slice tests for {@link NotificationSubscriberController}: mapping, bean
 * validation (400), JSON contract, {@code @ResponseStatus} exception mapping
 * (404/409), and delegation to the mocked {@link NotificationSubscriberService}.
 */
@WebMvcTest(controllers = NotificationSubscriberController.class)
class NotificationSubscriberControllerTest extends AbstractApiControllerTest {

    @MockitoBean
    private NotificationSubscriberService subscriberService;

    private NotificationSubscriberResponse sample(UUID id) {
        NotificationSubscriberResponse r = new NotificationSubscriberResponse();
        r.setId(id);
        r.setAppId(UUID.randomUUID());
        r.setEmail("sub@example.com");
        r.setName("Subscriber");
        r.setIsActive(true);
        r.setIsVerified(true);
        return r;
    }

    @Test
    void getAllSubscribers_noFilter_returnsOk() throws Exception {
        when(subscriberService.getAllSubscribers()).thenReturn(List.of(sample(UUID.randomUUID())));

        mockMvc.perform(get("/api/notification-subscribers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("sub@example.com"));
    }

    @Test
    void getAllSubscribers_withAppId_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(subscriberService.getSubscribersByAppId(appId)).thenReturn(List.of(sample(UUID.randomUUID())));

        mockMvc.perform(get("/api/notification-subscribers").param("appId", appId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("sub@example.com"));

        verify(subscriberService).getSubscribersByAppId(appId);
    }

    @Test
    void getSubscriberById_found_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriberService.getSubscriberById(id)).thenReturn(sample(id));

        mockMvc.perform(get("/api/notification-subscribers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getSubscriberById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriberService.getSubscriberById(id))
                .thenThrow(new ResourceNotFoundException("Subscriber not found with id: " + id));

        mockMvc.perform(get("/api/notification-subscribers/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSubscriber_valid_returns201() throws Exception {
        when(subscriberService.createSubscriber(any(), any(), any())).thenReturn(sample(UUID.randomUUID()));

        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"email\":\"sub@example.com\",\"name\":\"Subscriber\"}";
        mockMvc.perform(post("/api/notification-subscribers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("sub@example.com"));
    }

    @Test
    void createSubscriber_missingEmail_returns400() throws Exception {
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"name\":\"Subscriber\"}";
        mockMvc.perform(post("/api/notification-subscribers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSubscriber_invalidEmail_returns400() throws Exception {
        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"email\":\"not-an-email\"}";
        mockMvc.perform(post("/api/notification-subscribers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSubscriber_missingAppId_returns400() throws Exception {
        String body = "{\"email\":\"sub@example.com\"}";
        mockMvc.perform(post("/api/notification-subscribers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSubscriber_appNotFound_returns404() throws Exception {
        when(subscriberService.createSubscriber(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Status app not found"));

        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"email\":\"sub@example.com\"}";
        mockMvc.perform(post("/api/notification-subscribers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSubscriber_duplicate_returns409() throws Exception {
        when(subscriberService.createSubscriber(any(), any(), any()))
                .thenThrow(new DuplicateResourceException("Email already subscribed to this application"));

        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"email\":\"sub@example.com\"}";
        mockMvc.perform(post("/api/notification-subscribers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSubscriber_valid_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriberService.updateSubscriber(eq(id), any(), any(), any())).thenReturn(sample(id));

        String body = "{\"appId\":\"" + UUID.randomUUID() + "\",\"email\":\"new@example.com\",\"name\":\"New\",\"isActive\":false}";
        mockMvc.perform(put("/api/notification-subscribers/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deleteSubscriber_returnsOkMessage() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/notification-subscribers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(subscriberService).deleteSubscriber(id);
    }

    @Test
    void deleteSubscriber_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Subscriber not found with id: " + id))
                .when(subscriberService).deleteSubscriber(eq(id));

        mockMvc.perform(delete("/api/notification-subscribers/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSubscribersByApp_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(subscriberService.getSubscribersByAppId(appId)).thenReturn(List.of(sample(UUID.randomUUID())));

        mockMvc.perform(get("/api/notification-subscribers/by-app/{appId}", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("sub@example.com"));
    }

    @Test
    void getSubscriberCount_returnsOk() throws Exception {
        UUID appId = UUID.randomUUID();
        when(subscriberService.countSubscribersByAppId(appId)).thenReturn(7L);

        mockMvc.perform(get("/api/notification-subscribers/count/{appId}", appId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(7));
    }
}
