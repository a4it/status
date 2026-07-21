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

    @Test
    void create_regularUser_doesNotRequireContextSelection() {
        // Arrange
        User user = buildUser("jdoe", "USER", true);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.isRequiresContextSelection()).isFalse();
    }

    @Test
    void create_disabledUser_isEnabledReturnsFalse() {
        // Arrange
        User user = buildUser("jdoe", "USER", false);

        // Act
        UserPrincipal principal = UserPrincipal.create(user);

        // Assert
        assertThat(principal.isEnabled()).isFalse();
    }

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
