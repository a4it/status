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
 *
 * <p>Testing approach: pure Mockito unit tests. The subscriber and status-app repositories are
 * mocked and injected into the service so behaviour (read mapping, create/update/delete rules,
 * duplicate and not-found handling, and query delegation) is exercised without a database. A
 * {@link SecurityContextHolder} authentication is seeded per test to supply the "current user"
 * that the service stamps onto audit fields, and is cleared after each test.
 */
@ExtendWith(MockitoExtension.class)
class NotificationSubscriberServiceTest {

    @Mock
    private NotificationSubscriberRepository subscriberRepository;

    @Mock
    private StatusAppRepository statusAppRepository;

    @InjectMocks
    private NotificationSubscriberService service;

    /**
     * Seeds the security context with a "tester" authentication before each test so the service can
     * resolve the current username for audit fields such as createdBy/lastModifiedBy.
     */
    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, java.util.List.of()));
    }

    /**
     * Clears the security context after each test to avoid leaking authentication state between tests.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builds a minimal {@link StatusApp} fixture with the given id and a fixed name.
     *
     * @param id the identifier to assign to the app
     * @return a populated {@link StatusApp} for use in tests
     */
    private StatusApp app(UUID id) {
        StatusApp app = new StatusApp();
        app.setId(id);
        app.setName("Platform");
        return app;
    }

    /**
     * Builds an active, verified {@link NotificationSubscriber} fixture linked to the given app.
     *
     * @param id    the identifier to assign to the subscriber
     * @param app   the status app the subscriber belongs to
     * @param email the subscriber's email address
     * @return a populated {@link NotificationSubscriber} for use in tests
     */
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

    /**
     * Verifies that subscribers found for an app id are mapped to response objects carrying the
     * subscriber email plus the owning app's id and name.
     */
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

    /**
     * Verifies that {@code getAllSubscribers} returns a response for every subscriber found by the
     * repository.
     */
    @Test
    void getAllSubscribers_mapsToResponses() {
        StatusApp app = app(UUID.randomUUID());
        when(subscriberRepository.findAll())
                .thenReturn(List.of(subscriber(UUID.randomUUID(), app, "a@x.com"),
                        subscriber(UUID.randomUUID(), app, "b@x.com")));

        assertThat(service.getAllSubscribers()).hasSize(2);
    }

    /**
     * Verifies that fetching an existing subscriber by id returns a response with the matching id
     * and email.
     */
    @Test
    void getSubscriberById_whenFound_returnsResponse() {
        UUID id = UUID.randomUUID();
        StatusApp app = app(UUID.randomUUID());
        when(subscriberRepository.findById(id)).thenReturn(Optional.of(subscriber(id, app, "a@x.com")));

        NotificationSubscriberResponse response = service.getSubscriberById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getEmail()).isEqualTo("a@x.com");
    }

    /**
     * Verifies that fetching a non-existent subscriber by id throws {@link ResourceNotFoundException}
     * with a "Subscriber not found" message.
     */
    @Test
    void getSubscriberById_whenMissing_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubscriberById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Subscriber not found");
    }

    // ------------------------------------------------------------ createSubscriber

    /**
     * Verifies that creating a subscriber for an unknown app id throws
     * {@link ResourceNotFoundException} with a "Status app not found" message.
     */
    @Test
    void createSubscriber_whenAppNotFound_throwsResourceNotFound() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSubscriber(appId, "a@x.com", "Sub"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Status app not found");
    }

    /**
     * Verifies that creating a subscriber whose email is already subscribed to the app throws
     * {@link DuplicateResourceException} with an "already subscribed" message.
     */
    @Test
    void createSubscriber_whenEmailAlreadySubscribed_throwsDuplicate() {
        UUID appId = UUID.randomUUID();
        when(statusAppRepository.findById(appId)).thenReturn(Optional.of(app(appId)));
        when(subscriberRepository.existsByAppIdAndEmail(appId, "a@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createSubscriber(appId, "a@x.com", "Sub"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already subscribed");
    }

    /**
     * Verifies that a valid create request persists a subscriber with the correct app, email,
     * active/verified flags and audit fields set to the current user, and returns a matching response.
     */
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

    /**
     * Verifies that updating a non-existent subscriber throws {@link ResourceNotFoundException}.
     */
    @Test
    void updateSubscriber_whenMissing_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSubscriber(id, "n@x.com", "Name", true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that changing a subscriber's email to one already used for the same app throws
     * {@link DuplicateResourceException}.
     */
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

    /**
     * Verifies that a valid update mutates the subscriber's email, name and active flag, stamps the
     * current user as last modifier, and returns a response reflecting the new email.
     */
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

    /**
     * Verifies that updating a subscriber with an unchanged email skips the duplicate-email check
     * (no call to {@code existsByAppIdAndEmail}).
     */
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

    /**
     * Verifies that deleting a non-existent subscriber throws {@link ResourceNotFoundException} and
     * never invokes the repository delete.
     */
    @Test
    void deleteSubscriber_whenMissing_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteSubscriber(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriberRepository, never()).deleteById(any());
    }

    /**
     * Verifies that deleting an existing subscriber delegates to the repository's deleteById.
     */
    @Test
    void deleteSubscriber_whenExists_deletes() {
        UUID id = UUID.randomUUID();
        when(subscriberRepository.existsById(id)).thenReturn(true);

        service.deleteSubscriber(id);

        verify(subscriberRepository).deleteById(id);
    }

    // ------------------------------------------------------------ queries

    /**
     * Verifies that {@code getActiveVerifiedSubscribers} returns the repository's list of active,
     * verified subscribers unchanged.
     */
    @Test
    void getActiveVerifiedSubscribers_returnsRepositoryResult() {
        UUID appId = UUID.randomUUID();
        StatusApp app = app(appId);
        List<NotificationSubscriber> subs = List.of(subscriber(UUID.randomUUID(), app, "a@x.com"));
        when(subscriberRepository.findActiveVerifiedByAppId(appId)).thenReturn(subs);

        assertThat(service.getActiveVerifiedSubscribers(appId)).isEqualTo(subs);
    }

    /**
     * Verifies that {@code countSubscribersByAppId} returns the count reported by the repository.
     */
    @Test
    void countSubscribersByAppId_returnsRepositoryCount() {
        UUID appId = UUID.randomUUID();
        when(subscriberRepository.countByAppId(appId)).thenReturn(4L);

        assertThat(service.countSubscribersByAppId(appId)).isEqualTo(4L);
    }
}
