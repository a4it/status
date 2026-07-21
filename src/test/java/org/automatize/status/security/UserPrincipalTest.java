package org.automatize.status.security;

import org.automatize.status.models.Organization;
import org.automatize.status.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserPrincipal}.
 */
class UserPrincipalTest {

    /**
     * Builds a {@link User} fixture with a random id and derived email for use in
     * principal-creation tests.
     *
     * @param username the username (also used to derive the email)
     * @param role     the role string to assign
     * @param enabled  whether the user is enabled
     * @return the constructed {@link User}
     */
    private User buildUser(String username, String role, boolean enabled) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@x.com");
        user.setPassword("hashed");
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    /**
     * Verifies that {@code create} maps the user's role to a {@code ROLE_}-prefixed
     * authority and copies through the username, email, password, and role.
     */
    @Test
    void create_userWithRole_mapsAuthorityWithRolePrefix() {
        // Arrange
        User user = buildUser("jdoe", "ADMIN", true);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(principal.getRole()).isEqualTo("ADMIN");
        assertThat(principal.getUsername()).isEqualTo("jdoe");
        assertThat(principal.getEmail()).isEqualTo("jdoe@x.com");
        assertThat(principal.getPassword()).isEqualTo("hashed");
    }

    /**
     * Verifies that {@code create} defaults a user with a {@code null} role to the
     * {@code ROLE_USER} authority.
     */
    @Test
    void create_userWithNullRole_defaultsToRoleUser() {
        // Arrange
        User user = buildUser("jdoe", null, true);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    /**
     * Verifies that {@code create} populates the principal's organization id from
     * the user's associated organization.
     */
    @Test
    void create_userWithOrganization_populatesOrganizationId() {
        // Arrange
        User user = buildUser("jdoe", "USER", true);
        Organization org = new Organization();
        UUID orgId = UUID.randomUUID();
        org.setId(orgId);
        user.setOrganization(org);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.getOrganizationId()).isEqualTo(orgId);
    }

    /**
     * Verifies that a SUPERADMIN principal created via {@code create} requires
     * context selection and has not yet selected a context.
     */
    @Test
    void create_superadminUser_requiresContextSelection() {
        // Arrange
        User user = buildUser("root", "SUPERADMIN", true);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.isRequiresContextSelection()).isTrue();
        assertThat(principal.hasSelectedContext()).isFalse();
    }

    /**
     * Verifies that a regular USER principal created via {@code create} does not
     * require context selection.
     */
    @Test
    void create_regularUser_doesNotRequireContextSelection() {
        // Arrange
        User user = buildUser("jdoe", "USER", true);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.isRequiresContextSelection()).isFalse();
    }

    /**
     * Verifies that a principal created from a disabled user reports
     * {@code isEnabled() == false}.
     */
    @Test
    void create_disabledUser_isEnabledReturnsFalse() {
        // Arrange
        User user = buildUser("jdoe", "USER", false);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.isEnabled()).isFalse();
    }

    /**
     * Verifies that a principal created from an enabled user reports all account
     * status flags (enabled, non-expired, non-locked, credentials non-expired) as
     * {@code true}.
     */
    @Test
    void create_enabledUser_accountStatusFlagsAreTrue() {
        // Arrange
        User user = buildUser("jdoe", "USER", true);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
    }

    /**
     * Verifies that {@code createWithContext} stamps the principal with the given
     * tenant and organization ids, clears the context-selection requirement, and
     * marks a context as selected.
     */
    @Test
    void createWithContext_setsTenantAndOrgAndSelectedContext() {
        // Arrange
        User user = buildUser("root", "SUPERADMIN", true);
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        // Act
        UserPrincipal principal = UserPrincipal.createWithContext(user, tenantId, orgId);

        // Assert
        assertThat(principal.getTenantId()).isEqualTo(tenantId);
        assertThat(principal.getOrganizationId()).isEqualTo(orgId);
        assertThat(principal.isRequiresContextSelection()).isFalse();
        assertThat(principal.hasSelectedContext()).isTrue();
    }
}
