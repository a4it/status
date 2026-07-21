package org.automatize.status.services;

import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.UnauthorizedException;
import org.automatize.status.models.PlatformEvent;
import org.automatize.status.models.StatusApp;
import org.automatize.status.models.StatusComponent;
import org.automatize.status.repositories.PlatformEventRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.automatize.status.repositories.StatusComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlatformEventService}.
 */
@ExtendWith(MockitoExtension.class)
class PlatformEventServiceTest {

    @Mock
    private PlatformEventRepository platformEventRepository;
    @Mock
    private StatusAppRepository statusAppRepository;
    @Mock
    private StatusComponentRepository statusComponentRepository;

    @InjectMocks
    private PlatformEventService platformEventService;

    private StatusApp newApp(UUID id) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("App");
        return app;
    }

    private StatusComponent newComponent(UUID id, StatusApp app) {
        StatusComponent component = new StatusComponent();
        component.setId(id);
        component.setApp(app);
        component.setName("Comp");
        return component;
    }

    @Test
    void validateAppApiKey_validKey_returnsApp() {
        StatusApp app = newApp(UUID.randomUUID());
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.of(app));

        StatusApp result = platformEventService.validateAppApiKey("key");

        assertThat(result).isEqualTo(app);
    }

    @Test
    void validateAppApiKey_nullKey_throwsBusinessRule() {
        assertThatThrownBy(() -> platformEventService.validateAppApiKey(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validateAppApiKey_notFound_throwsUnauthorized() {
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.validateAppApiKey("key"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validateComponentApiKey_validKey_returnsComponent() {
        StatusComponent component = newComponent(UUID.randomUUID(), newApp(UUID.randomUUID()));
        when(statusComponentRepository.findByApiKey("key")).thenReturn(Optional.of(component));

        StatusComponent result = platformEventService.validateComponentApiKey("key");

        assertThat(result).isEqualTo(component);
    }

    @Test
    void validateComponentApiKey_emptyKey_throwsBusinessRule() {
        assertThatThrownBy(() -> platformEventService.validateComponentApiKey(""))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validateComponentApiKey_notFound_throwsUnauthorized() {
        when(statusComponentRepository.findByApiKey("key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.validateComponentApiKey("key"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void createEventWithApiKey_appKey_savesEventForApp() {
        StatusApp app = newApp(UUID.randomUUID());
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.of(app));
        when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        PlatformEvent event = platformEventService.createEventWithApiKey("key", "ERROR", "src", "msg", "details", null);

        assertThat(event.getApp()).isEqualTo(app);
        assertThat(event.getComponent()).isNull();
        assertThat(event.getSeverity()).isEqualTo("ERROR");
        assertThat(event.getEventTime()).isNotNull();
    }

    @Test
    void createEventWithApiKey_componentKey_savesEventForComponentApp() {
        StatusApp app = newApp(UUID.randomUUID());
        StatusComponent component = newComponent(UUID.randomUUID(), app);
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.empty());
        when(statusComponentRepository.findByApiKey("key")).thenReturn(Optional.of(component));
        when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        PlatformEvent event = platformEventService.createEventWithApiKey("key", "INFO", null, "msg", null, null);

        assertThat(event.getComponent()).isEqualTo(component);
        assertThat(event.getApp()).isEqualTo(app);
    }

    @Test
    void createEventWithApiKey_invalidKey_throwsUnauthorized() {
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.empty());
        when(statusComponentRepository.findByApiKey("key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.createEventWithApiKey("key", "INFO", null, "msg", null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void createEvent_noComponent_savesEvent() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        PlatformEvent event = platformEventService.createEvent(appId, null, "WARNING", "src", "msg", null, null);

        assertThat(event.getApp()).isEqualTo(app);
        assertThat(event.getComponent()).isNull();
    }

    @Test
    void createEvent_componentBelongsToApp_savesEvent() {
        UUID appId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        StatusComponent component = newComponent(componentId, app);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(platformEventRepository.save(any(PlatformEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        PlatformEvent event = platformEventService.createEvent(appId, componentId, "INFO", null, "msg", null, null);

        assertThat(event.getComponent()).isEqualTo(component);
    }

    @Test
    void createEvent_componentBelongsToDifferentApp_throwsBusinessRule() {
        UUID appId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        StatusComponent component = newComponent(componentId, newApp(UUID.randomUUID()));
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.of(component));

        assertThatThrownBy(() -> platformEventService.createEvent(appId, componentId, "INFO", null, "msg", null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createEvent_appNotFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.createEvent(appId, null, "INFO", null, "msg", null, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void createEvent_componentNotFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(newApp(appId)));
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.createEvent(appId, componentId, "INFO", null, "msg", null, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getEventById_found_returnsEvent() {
        UUID id = UUID.randomUUID();
        PlatformEvent event = new PlatformEvent();
        event.setId(id);
        when(platformEventRepository.findById(id)).thenReturn(Optional.of(event));

        assertThat(platformEventService.getEventById(id)).isEqualTo(event);
    }

    @Test
    void getEventById_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(platformEventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.getEventById(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getEvents_delegatesToRepositoryWithFilters() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("eventTime"));
        when(platformEventRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new PlatformEvent())));

        var page = platformEventService.getEvents(null, null, null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void searchEvents_withText_usesSearchWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(platformEventRepository.searchWithFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new PlatformEvent())));

        var page = platformEventService.searchEvents(null, null, null, null, null, "boom", pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(platformEventRepository, never()).findWithFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchEvents_blankText_delegatesToGetEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        when(platformEventRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        platformEventService.searchEvents(null, null, null, null, null, "   ", pageable);

        verify(platformEventRepository).findWithFilters(any(), any(), any(), any(), any(), any());
        verify(platformEventRepository, never()).searchWithFilters(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteEvent_found_deletes() {
        UUID id = UUID.randomUUID();
        PlatformEvent event = new PlatformEvent();
        when(platformEventRepository.findById(id)).thenReturn(Optional.of(event));

        platformEventService.deleteEvent(id);

        verify(platformEventRepository).delete(event);
    }

    @Test
    void deleteEvent_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(platformEventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.deleteEvent(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteEventsByAppId_delegatesToRepository() {
        UUID appId = UUID.randomUUID();

        platformEventService.deleteEventsByAppId(appId);

        verify(platformEventRepository).deleteByAppId(appId);
    }

    @Test
    void countEventsByAppId_returnsCount() {
        UUID appId = UUID.randomUUID();
        when(platformEventRepository.countByAppId(appId)).thenReturn(7L);

        assertThat(platformEventService.countEventsByAppId(appId)).isEqualTo(7L);
    }

    @Test
    void regenerateComponentApiKey_updatesAndReturnsNewKey() {
        UUID componentId = UUID.randomUUID();
        StatusComponent component = newComponent(componentId, newApp(UUID.randomUUID()));
        component.setApiKey("old");
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(statusComponentRepository.save(any(StatusComponent.class))).thenAnswer(inv -> inv.getArgument(0));

        String newKey = platformEventService.regenerateComponentApiKey(componentId);

        assertThat(newKey).isNotBlank().isNotEqualTo("old");
        assertThat(component.getApiKey()).isEqualTo(newKey);
    }

    @Test
    void regenerateComponentApiKey_notFound_throwsRuntime() {
        UUID componentId = UUID.randomUUID();
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.regenerateComponentApiKey(componentId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void regenerateAppApiKey_updatesAndReturnsNewKey() {
        UUID appId = UUID.randomUUID();
        StatusApp app = newApp(appId);
        app.setApiKey("old");
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(statusAppRepository.save(any(StatusApp.class))).thenAnswer(inv -> inv.getArgument(0));

        String newKey = platformEventService.regenerateAppApiKey(appId);

        assertThat(newKey).isNotBlank().isNotEqualTo("old");
        assertThat(app.getApiKey()).isEqualTo(newKey);
    }

    @Test
    void regenerateAppApiKey_notFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.regenerateAppApiKey(appId))
                .isInstanceOf(RuntimeException.class);
    }
}
