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
 *
 * <p>Testing approach: pure Mockito unit tests. The platform-event, status-app and status-component
 * repositories are mocked and injected into the service, so API-key validation, event creation (via
 * API key or explicit ids), component-to-app ownership rules, lookups, search/filter delegation,
 * deletion, counting and API-key regeneration are verified without a database.
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

    /**
     * Builds a minimal {@link StatusApp} fixture with the given id and a fixed name.
     *
     * @param id the identifier to assign to the app
     * @return a populated {@link StatusApp}
     */
    private StatusApp newApp(UUID id) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("App");
        return app;
    }

    /**
     * Builds a minimal {@link StatusComponent} fixture with the given id, owning app and a fixed name.
     *
     * @param id  the identifier to assign to the component
     * @param app the status app the component belongs to
     * @return a populated {@link StatusComponent}
     */
    private StatusComponent newComponent(UUID id, StatusApp app) {
        StatusComponent component = new StatusComponent();
        component.setId(id);
        component.setApp(app);
        component.setName("Comp");
        return component;
    }

    /**
     * Verifies that validating a known app API key returns the matching {@link StatusApp}.
     */
    @Test
    void validateAppApiKey_validKey_returnsApp() {
        StatusApp app = newApp(UUID.randomUUID());
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.of(app));

        StatusApp result = platformEventService.validateAppApiKey("key");

        assertThat(result).isEqualTo(app);
    }

    /**
     * Verifies that validating a {@code null} app API key throws {@link BusinessRuleException}.
     */
    @Test
    void validateAppApiKey_nullKey_throwsBusinessRule() {
        assertThatThrownBy(() -> platformEventService.validateAppApiKey(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    /**
     * Verifies that validating an unknown app API key throws {@link UnauthorizedException}.
     */
    @Test
    void validateAppApiKey_notFound_throwsUnauthorized() {
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.validateAppApiKey("key"))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * Verifies that validating a known component API key returns the matching
     * {@link StatusComponent}.
     */
    @Test
    void validateComponentApiKey_validKey_returnsComponent() {
        StatusComponent component = newComponent(UUID.randomUUID(), newApp(UUID.randomUUID()));
        when(statusComponentRepository.findByApiKey("key")).thenReturn(Optional.of(component));

        StatusComponent result = platformEventService.validateComponentApiKey("key");

        assertThat(result).isEqualTo(component);
    }

    /**
     * Verifies that validating an empty component API key throws {@link BusinessRuleException}.
     */
    @Test
    void validateComponentApiKey_emptyKey_throwsBusinessRule() {
        assertThatThrownBy(() -> platformEventService.validateComponentApiKey(""))
                .isInstanceOf(BusinessRuleException.class);
    }

    /**
     * Verifies that validating an unknown component API key throws {@link UnauthorizedException}.
     */
    @Test
    void validateComponentApiKey_notFound_throwsUnauthorized() {
        when(statusComponentRepository.findByApiKey("key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.validateComponentApiKey("key"))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * Verifies that creating an event with an app API key saves an event bound to that app, with no
     * component, the given severity and a populated event time.
     */
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

    /**
     * Verifies that when the key does not match an app but matches a component, the event is saved
     * bound to that component and its owning app.
     */
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

    /**
     * Verifies that creating an event with a key matching neither an app nor a component throws
     * {@link UnauthorizedException}.
     */
    @Test
    void createEventWithApiKey_invalidKey_throwsUnauthorized() {
        when(statusAppRepository.findByApiKey("key")).thenReturn(Optional.empty());
        when(statusComponentRepository.findByApiKey("key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.createEventWithApiKey("key", "INFO", null, "msg", null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * Verifies that creating an event by app id with no component saves an event bound to the app and
     * with a null component.
     */
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

    /**
     * Verifies that creating an event with a component that belongs to the given app saves an event
     * bound to that component.
     */
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

    /**
     * Verifies that creating an event whose component belongs to a different app throws
     * {@link BusinessRuleException}.
     */
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

    /**
     * Verifies that creating an event for an unknown app id throws a {@link RuntimeException}.
     */
    @Test
    void createEvent_appNotFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.createEvent(appId, null, "INFO", null, "msg", null, null))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that creating an event with an unknown component id (for a known app) throws a
     * {@link RuntimeException}.
     */
    @Test
    void createEvent_componentNotFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        UUID componentId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(newApp(appId)));
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.createEvent(appId, componentId, "INFO", null, "msg", null, null))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that fetching an existing event by id returns that event.
     */
    @Test
    void getEventById_found_returnsEvent() {
        UUID id = UUID.randomUUID();
        PlatformEvent event = new PlatformEvent();
        event.setId(id);
        when(platformEventRepository.findById(id)).thenReturn(Optional.of(event));

        assertThat(platformEventService.getEventById(id)).isEqualTo(event);
    }

    /**
     * Verifies that fetching a non-existent event by id throws a {@link RuntimeException}.
     */
    @Test
    void getEventById_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(platformEventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.getEventById(id))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that {@code getEvents} delegates to the repository's filtered query and returns its
     * page of results.
     */
    @Test
    void getEvents_delegatesToRepositoryWithFilters() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("eventTime"));
        when(platformEventRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new PlatformEvent())));

        var page = platformEventService.getEvents(null, null, null, null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    /**
     * Verifies that searching with non-blank text routes to the repository's search-with-filters
     * query and does not call the plain filtered query.
     */
    @Test
    void searchEvents_withText_usesSearchWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(platformEventRepository.searchWithFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new PlatformEvent())));

        var page = platformEventService.searchEvents(null, null, null, null, null, "boom", pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(platformEventRepository, never()).findWithFilters(any(), any(), any(), any(), any(), any());
    }

    /**
     * Verifies that searching with blank text falls back to the plain filtered query and never calls
     * the search-with-filters query.
     */
    @Test
    void searchEvents_blankText_delegatesToGetEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        when(platformEventRepository.findWithFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        platformEventService.searchEvents(null, null, null, null, null, "   ", pageable);

        verify(platformEventRepository).findWithFilters(any(), any(), any(), any(), any(), any());
        verify(platformEventRepository, never()).searchWithFilters(any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * Verifies that deleting an existing event delegates to the repository's delete for that event.
     */
    @Test
    void deleteEvent_found_deletes() {
        UUID id = UUID.randomUUID();
        PlatformEvent event = new PlatformEvent();
        when(platformEventRepository.findById(id)).thenReturn(Optional.of(event));

        platformEventService.deleteEvent(id);

        verify(platformEventRepository).delete(event);
    }

    /**
     * Verifies that deleting a non-existent event throws a {@link RuntimeException}.
     */
    @Test
    void deleteEvent_notFound_throwsRuntime() {
        UUID id = UUID.randomUUID();
        when(platformEventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.deleteEvent(id))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that {@code deleteEventsByAppId} delegates to the repository's bulk delete-by-app-id.
     */
    @Test
    void deleteEventsByAppId_delegatesToRepository() {
        UUID appId = UUID.randomUUID();

        platformEventService.deleteEventsByAppId(appId);

        verify(platformEventRepository).deleteByAppId(appId);
    }

    /**
     * Verifies that {@code countEventsByAppId} returns the count reported by the repository.
     */
    @Test
    void countEventsByAppId_returnsCount() {
        UUID appId = UUID.randomUUID();
        when(platformEventRepository.countByAppId(appId)).thenReturn(7L);

        assertThat(platformEventService.countEventsByAppId(appId)).isEqualTo(7L);
    }

    /**
     * Verifies that regenerating a component's API key produces a new non-blank key that differs from
     * the old one and is persisted on the component.
     */
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

    /**
     * Verifies that regenerating the API key for an unknown component id throws a
     * {@link RuntimeException}.
     */
    @Test
    void regenerateComponentApiKey_notFound_throwsRuntime() {
        UUID componentId = UUID.randomUUID();
        when(statusComponentRepository.findById(componentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.regenerateComponentApiKey(componentId))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Verifies that regenerating an app's API key produces a new non-blank key that differs from the
     * old one and is persisted on the app.
     */
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

    /**
     * Verifies that regenerating the API key for an unknown app id throws a {@link RuntimeException}.
     */
    @Test
    void regenerateAppApiKey_notFound_throwsRuntime() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> platformEventService.regenerateAppApiKey(appId))
                .isInstanceOf(RuntimeException.class);
    }
}
