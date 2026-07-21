package org.automatize.status.security;

import org.automatize.status.models.Organization;
import org.automatize.status.models.User;
import org.automatize.status.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User buildUser(UUID id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@x.com");
        user.setPassword("hashed");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    @Test
    void loadUserByUsername_userFound_returnsUserPrincipal() {
        // Arrange
        User user = buildUser(UUID.randomUUID(), "jdoe", "USER");
        when(userRepository.findByUsernameOrEmail("jdoe", "jdoe")).thenReturn(Optional.of(user));

        // Act
        UserDetails details = service.loadUserByUsername("jdoe");

        // Assert
        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(details.getUsername()).isEqualTo("jdoe");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("ghost", "ghost")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void loadUserById_userFound_returnsUserPrincipal() {
        // Arrange
        UUID id = UUID.randomUUID();
        User user = buildUser(id, "jdoe", "USER");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act
        UserDetails details = service.loadUserById(id);

        // Assert
        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) details).getId()).isEqualTo(id);
    }

    @Test
    void loadUserById_userNotFound_throwsUsernameNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.loadUserById(id))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void loadUserByIdWithContext_userFound_returnsPrincipalWithContext() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User user = buildUser(id, "root", "SUPERADMIN");
        Organization org = new Organization();
        org.setId(orgId);
        user.setOrganization(org);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // Act
        UserDetails details = service.loadUserByIdWithContext(id, tenantId, orgId);

        // Assert
        assertThat(details).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) details;
        assertThat(principal.getTenantId()).isEqualTo(tenantId);
        assertThat(principal.getOrganizationId()).isEqualTo(orgId);
        assertThat(principal.isRequiresContextSelection()).isFalse();
    }

    @Test
    void loadUserByIdWithContext_userNotFound_throwsUsernameNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.loadUserByIdWithContext(id, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
