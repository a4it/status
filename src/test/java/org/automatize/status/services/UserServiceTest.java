package org.automatize.status.services;

import org.automatize.status.api.request.PasswordChangeRequest;
import org.automatize.status.api.request.UserRequest;
import org.automatize.status.exceptions.BusinessRuleException;
import org.automatize.status.exceptions.DuplicateResourceException;
import org.automatize.status.exceptions.ResourceNotFoundException;
import org.automatize.status.models.Organization;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
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
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID currentUserId;

    /**
     * Initialises a random current-user id and installs an authenticated ADMIN principal
     * in the security context before each test.
     */
    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        setPrincipal(currentUserId, "ADMIN");
    }

    /**
     * Clears the security context after each test to avoid leaking authentication
     * state between tests.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Installs an authenticated {@link UserPrincipal} with the given id and role into
     * the security context.
     *
     * @param id   the principal user identifier
     * @param role the principal security role
     */
    private void setPrincipal(UUID id, String role) {
        UserPrincipal principal = new UserPrincipal(
                id, "tester", "tester@example.com", "pw",
                role, UUID.randomUUID(), true, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
    }

    /**
     * Builds an enabled {@link User} test fixture with the given identifier and username,
     * deriving the email from the username and using a fixed stored password hash.
     *
     * @param id       the user identifier to assign
     * @param username the username to assign
     * @return a populated {@link User} instance for use in tests
     */
    private User buildUser(UUID id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("stored-hash");
        user.setRole("USER");
        user.setEnabled(true);
        return user;
    }

    /**
     * Builds a {@link UserRequest} test fixture carrying the given username and email.
     *
     * @param username the username to set on the request
     * @param email    the email to set on the request
     * @return a populated {@link UserRequest} instance for use in tests
     */
    private UserRequest buildRequest(String username, String email) {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setEmail(email);
        return request;
    }

    // ---------- getAllUsers ----------

    /**
     * Verifies that {@code getAllUsers} filters by both organization and role when both
     * are supplied.
     */
    @Test
    void getAllUsers_organizationAndRole_filtersByBoth() {
        UUID orgId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByOrganizationIdAndRole(orgId, "ADMIN"))
                .thenReturn(List.of(buildUser(UUID.randomUUID(), "a")));

        Page<User> result = userService.getAllUsers(orgId, "ADMIN", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getAllUsers} filters by both organization and enabled flag
     * when both are supplied.
     */
    @Test
    void getAllUsers_organizationAndEnabled_filtersByBoth() {
        UUID orgId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByOrganizationIdAndEnabled(orgId, true))
                .thenReturn(List.of(buildUser(UUID.randomUUID(), "a")));

        Page<User> result = userService.getAllUsers(orgId, null, true, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getAllUsers} filters by organization only when just the
     * organization id is supplied.
     */
    @Test
    void getAllUsers_organizationOnly_filtersByOrganization() {
        UUID orgId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByOrganizationId(orgId))
                .thenReturn(List.of(buildUser(UUID.randomUUID(), "a")));

        Page<User> result = userService.getAllUsers(orgId, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getAllUsers} filters by role only when just the role is supplied.
     */
    @Test
    void getAllUsers_roleOnly_filtersByRole() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByRole("ADMIN"))
                .thenReturn(List.of(buildUser(UUID.randomUUID(), "a")));

        Page<User> result = userService.getAllUsers(null, "ADMIN", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getAllUsers} filters by enabled flag only when just the
     * enabled flag is supplied.
     */
    @Test
    void getAllUsers_enabledOnly_filtersByEnabled() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByEnabled(false))
                .thenReturn(List.of(buildUser(UUID.randomUUID(), "a")));

        Page<User> result = userService.getAllUsers(null, null, false, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getAllUsers} performs a search query when only a search term
     * is supplied.
     */
    @Test
    void getAllUsers_searchOnly_usesSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.search("bob"))
                .thenReturn(List.of(buildUser(UUID.randomUUID(), "bob")));

        Page<User> result = userService.getAllUsers(null, null, null, "bob", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    /**
     * Verifies that {@code getAllUsers} returns the full paged result from {@code findAll}
     * when no filters or search term are supplied.
     */
    @Test
    void getAllUsers_noFilters_returnsFindAllPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(buildUser(UUID.randomUUID(), "a")));
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.getAllUsers(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ---------- getUserById / lookups ----------

    /**
     * Verifies that {@code getUserById} returns the user when one exists for the id.
     */
    @Test
    void getUserById_existing_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, "a");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThat(userService.getUserById(id)).isSameAs(user);
    }

    /**
     * Verifies that {@code getUserById} throws {@link ResourceNotFoundException} when no
     * user exists for the id.
     */
    @Test
    void getUserById_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that {@code getUsersByOrganization} delegates to the repository and returns
     * the users it provides for the organization.
     */
    @Test
    void getUsersByOrganization_delegatesToRepository() {
        UUID orgId = UUID.randomUUID();
        List<User> users = List.of(buildUser(UUID.randomUUID(), "a"));
        when(userRepository.findByOrganizationId(orgId)).thenReturn(users);

        assertThat(userService.getUsersByOrganization(orgId)).isEqualTo(users);
    }

    /**
     * Verifies that {@code getCurrentUserProfile} returns the user identified by the
     * authenticated principal.
     */
    @Test
    void getCurrentUserProfile_returnsCurrentUser() {
        User user = buildUser(currentUserId, "tester");
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));

        assertThat(userService.getCurrentUserProfile()).isSameAs(user);
    }

    // ---------- createUser ----------

    @Test
    void createUser_uniqueUsernameAndEmail_savesUser() {
        UserRequest request = buildRequest("newuser", "new@example.com");
        request.setPassword("pw");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser(request);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getPassword()).isEqualTo("hashed");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateUsername_throwsDuplicateResourceException() {
        UserRequest request = buildRequest("dup", "new@example.com");
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_duplicateEmail_throwsDuplicateResourceException() {
        UserRequest request = buildRequest("newuser", "dup@example.com");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_withOrganization_associatesOrganization() {
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);

        UserRequest request = buildRequest("newuser", "new@example.com");
        request.setOrganizationId(orgId);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser(request);

        assertThat(result.getOrganization()).isSameAs(org);
    }

    @Test
    void createUser_organizationNotFound_throwsResourceNotFoundException() {
        UUID orgId = UUID.randomUUID();
        UserRequest request = buildRequest("newuser", "new@example.com");
        request.setOrganizationId(orgId);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- updateUser ----------

    @Test
    void updateUser_selfUpdate_updatesBasicFields() {
        UUID id = UUID.randomUUID();
        setPrincipal(id, "USER");
        User user = buildUser(id, "self");

        UserRequest request = buildRequest("self", "self@example.com");
        request.setFullName("New Name");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(id, request);

        assertThat(result.getFullName()).isEqualTo("New Name");
    }

    @Test
    void updateUser_otherUserAsNonAdmin_throwsAccessDeniedException() {
        UUID targetId = UUID.randomUUID();
        setPrincipal(UUID.randomUUID(), "USER");
        User user = buildUser(targetId, "target");

        UserRequest request = buildRequest("target", "target@example.com");
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUser(targetId, request))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_duplicateUsername_throwsDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        setPrincipal(id, "USER");
        User user = buildUser(id, "self");

        UserRequest request = buildRequest("taken", "self@example.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(id, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateUser_duplicateEmail_throwsDuplicateResourceException() {
        UUID id = UUID.randomUUID();
        setPrincipal(id, "USER");
        User user = buildUser(id, "self");

        UserRequest request = buildRequest("self", "taken@example.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(id, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateUser_adminChangesPasswordAndOrganization() {
        UUID targetId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        setPrincipal(UUID.randomUUID(), "ADMIN");
        User user = buildUser(targetId, "target");
        Organization org = new Organization();
        org.setId(orgId);

        UserRequest request = buildRequest("target", "target@example.com");
        request.setPassword("newpw");
        request.setOrganizationId(orgId);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpw")).thenReturn("hashed-new");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(targetId, request);

        assertThat(result.getPassword()).isEqualTo("hashed-new");
        assertThat(result.getOrganization()).isSameAs(org);
    }

    // ---------- updateCurrentUserProfile ----------

    @Test
    void updateCurrentUserProfile_validRequest_updatesProfile() {
        User user = buildUser(currentUserId, "tester");

        UserRequest request = buildRequest("tester", "tester@example.com");
        request.setFullName("Full Name");
        request.setType("STANDARD");

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateCurrentUserProfile(request);

        assertThat(result.getFullName()).isEqualTo("Full Name");
        assertThat(result.getType()).isEqualTo("STANDARD");
    }

    @Test
    void updateCurrentUserProfile_duplicateUsername_throwsDuplicateResourceException() {
        User user = buildUser(currentUserId, "tester");

        UserRequest request = buildRequest("taken", "tester@example.com");
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUserProfile(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateCurrentUserProfile_duplicateEmail_throwsDuplicateResourceException() {
        User user = buildUser(currentUserId, "tester");

        UserRequest request = buildRequest("tester", "taken@example.com");
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUserProfile(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_selfWithCorrectCurrentPassword_updatesPassword() {
        UUID id = UUID.randomUUID();
        setPrincipal(id, "USER");
        User user = buildUser(id, "self");

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("new");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("hashed-new");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword(id, request);

        assertThat(user.getPassword()).isEqualTo("hashed-new");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_selfWithWrongCurrentPassword_throwsBusinessRuleException() {
        UUID id = UUID.randomUUID();
        setPrincipal(id, "USER");
        User user = buildUser(id, "self");

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("new");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(id, request))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_otherUserAsNonAdmin_throwsAccessDeniedException() {
        UUID targetId = UUID.randomUUID();
        setPrincipal(UUID.randomUUID(), "USER");
        User user = buildUser(targetId, "target");

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setNewPassword("new");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(targetId, request))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_adminForOtherUser_skipsCurrentPasswordCheck() {
        UUID targetId = UUID.randomUUID();
        setPrincipal(UUID.randomUUID(), "ADMIN");
        User user = buildUser(targetId, "target");

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setNewPassword("new");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new")).thenReturn("hashed-new");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword(targetId, request);

        assertThat(user.getPassword()).isEqualTo("hashed-new");
    }

    // ---------- enable / disable / role / delete ----------

    @Test
    void enableUser_setsEnabledAndActive() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, "a");
        user.setEnabled(false);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.enableUser(id);

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void disableUser_setsDisabledAndInactive() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, "a");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.disableUser(id);

        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void updateRole_setsRole() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, "a");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateRole(id, "ADMIN");

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void deleteUser_existing_deletesUser() {
        UUID id = UUID.randomUUID();
        User user = buildUser(id, "a");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.deleteUser(id);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_missing_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).delete(any());
    }
}
