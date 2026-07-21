package org.automatize.status.services;

import org.automatize.status.api.response.NotificationSubscriberResponse;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.NotificationSubscriber;
import org.automatize.status.models.StatusApp;
import org.automatize.status.repositories.NotificationSubscriberRepository;
import org.automatize.status.repositories.StatusAppRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
 * Unit tests for {@link NotificationSubscriberService}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationSubscriberServiceTest {

    @Mock
    private NotificationSubscriberRepository subscriberRepository;

    @Mock
    private StatusAppRepository statusAppRepository;

    @InjectMocks
    private NotificationSubscriberService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, java.util.List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private StatusApp app(UUID id) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("Platform");
        return app;
    }

    private NotificationSubscriber subscriber(UUID id, StatusApp app, String email) {
        NotificationSubscriber sub = new NotificationSubscriber();
        sub.setId(id);
        sub.setApp(app);
        sub.setEmail(email);
        sub.setName("Sub");
        sub.setIsActive(true);
        sub.setIsVerified(true);
        return sub;
    }

    // ----------------------------------------------------------------- reads

    @Test
    void getSubscribersByAppId_mapsToResponses() {
        UUID appId = UUID.randomUUID();
        StatusApp app = app(appId);
        when(subscriberRepository.findByAppId(appId))
                .thenReturn(List.of(subscriber(UUID.randomUUID(), app, "a@x.com")));

        List<NotificationSubscriberResponse> result = service.getSubscribersByAppId(appId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("a@x.com");
        assertThat(result.get(0).getAppId()).isEqualTo(appId);
        assertThat(result.get(0).getAppName()).isEqualTo("Platform");
    }

    @Test
    void getAllSubscribers_mapsToResponses() {
        StatusApp app = app(UUID.randomUUID());
        when(subscriberRepository.findAll())
                .thenReturn(List.of(subscriber(UUID.randomUUID(), app, "a@x.com"),
                        subscriber(UUID.randomUUID(), app, "b@x.com")));

        assertThat(service.getAllSubscribers()).hasSize(2);
    }

    @Test
    void getSubscriberById_whenFound_returnsResponse() {
        UUID id = UUID.randomUUID();
        StatusApp app = app(UUID.randomUUID());
        when(subscriberRepository.findById(id)).thenReturn(Optional.of(subscriber(id, app, "a@x.com")));

        NotificationSubscriberResponse response = service.getSubscriberById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEmail()).isEqualTo("a@x.com");
    }

    @Test
    void getSubscriberById_whenMissing_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubscriberById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscriber not found");
    }

    // ------------------------------------------------------------ createSubscriber

    @Test
    void createSubscriber_whenAppNotFound_throwsResourceNotFound() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSubscriber(appId, "a@x.com", "Sub"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Status app not found");
    }

    @Test
    void createSubscriber_whenEmailAlreadySubscribed_throwsDuplicate() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app(appId)));
        when(subscriberRepository.existsByAppIdAndEmail(appId, "a@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createSubscriber(appId, "a@x.com", "Sub"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already subscribed");
    }

    @Test
    void createSubscriber_whenValid_savesWithCurrentUserAndReturnsResponse() {
        UUID appId = UUID.randomUUID();
        StatusApp app = app(appId);
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app));
        when(subscriberRepository.existsByAppIdAndEmail(appId, "a@x.com")).thenReturn(false);
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationSubscriberResponse response = service.createSubscriber(appId, "a@x.com", "Sub");

        ArgumentCaptor<NotificationSubscriber> captor = ArgumentCaptor.forClass(NotificationSubscriber.class);
        verify(subscriberRepository).save(captor.capture());
        NotificationSubscriber saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("a@x.com");
        assertThat(saved.getApp()).isSameAs(app);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getIsVerified()).isTrue();
        assertThat(saved.getCreatedBy()).isEqualTo("tester");
        assertThat(saved.getLastModifiedBy()).isEqualTo("tester");
        assertThat(response.getEmail()).isEqualTo("a@x.com");
    }

    // ------------------------------------------------------------ updateSubscriber

    @Test
    void updateSubscriber_whenMissing_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSubscriber(id, "n@x.com", "Name", true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSubscriber_whenNewEmailAlreadyExists_throwsDuplicate() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = app(appId);
        NotificationSubscriber existing = subscriber(id, app, "old@x.com");
        when(subscriberRepository.findById(id)).thenReturn(Optional.of(existing));
        when(subscriberRepository.existsByAppIdAndEmail(appId, "new@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.updateSubscriber(id, "new@x.com", null, null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateSubscriber_whenValid_updatesFields() {
        UUID id = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        StatusApp app = app(appId);
        NotificationSubscriber existing = subscriber(id, app, "old@x.com");
        when(subscriberRepository.findById(id)).thenReturn(Optional.of(existing));
        when(subscriberRepository.existsByAppIdAndEmail(appId, "new@x.com")).thenReturn(false);
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationSubscriberResponse response = service.updateSubscriber(id, "new@x.com", "NewName", false);

        assertThat(existing.getEmail()).isEqualTo("new@x.com");
        assertThat(existing.getName()).isEqualTo("NewName");
        assertThat(existing.getIsActive()).isFalse();
        assertThat(existing.getLastModifiedBy()).isEqualTo("tester");
        assertThat(response.getEmail()).isEqualTo("new@x.com");
    }

    @Test
    void updateSubscriber_whenSameEmail_doesNotCheckDuplicate() {
        UUID id = UUID.randomUUID();
        StatusApp app = app(UUID.randomUUID());
        NotificationSubscriber existing = subscriber(id, app, "same@x.com");
        when(subscriberRepository.findById(id)).thenReturn(Optional.of(existing));
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateSubscriber(id, "same@x.com", null, null);

        verify(subscriberRepository, never()).existsByAppIdAndEmail(any(), any());
    }

    // ------------------------------------------------------------ deleteSubscriber

    @Test
    void deleteSubscriber_whenMissing_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteSubscriber(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriberRepository, never()).deleteById(any());
    }

    @Test
    void deleteSubscriber_whenExists_deletes() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.existsById(id)).thenReturn(true);

        service.deleteSubscriber(id);

        verify(subscriberRepository).deleteById(id);
    }

    // ------------------------------------------------------------ queries

    @Test
    void getActiveVerifiedSubscribers_returnsRepositoryResult() {
        UUID appId = UUID.randomUUID();
        StatusApp app = app(appId);
        List<NotificationSubscriber> subs = List.of(subscriber(UUID.randomUUID(), app, "a@x.com"));
        when(subscriberRepository.findActiveVerifiedByAppId(appId)).thenReturn(subs);

        assertThat(service.getActiveVerifiedSubscribers(appId)).isEqualTo(subs);
    }

    @Test
    void countSubscribersByAppId_returnsRepositoryCount() {
        UUID appId = UUID.randomUUID();
        when(subscriberRepository.countByAppId(appId)).thenReturn(4L);

        assertThat(service.countSubscribersByAppId(appId)).isEqualTo(4L);
    }
}
