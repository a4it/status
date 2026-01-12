package org.automatize.status.services;

import org.automatize.status.api.request.PasswordChangeRequest;
import org.automatize.status.api.request.UserRequest;
import org.automatize.status.models.Organization;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(UUID organizationId, String role, Boolean enabled, String search, Pageable pageable) {
        List<User> users;
        
        if (organizationId != null && role != null) {
            users = userRepository.findByOrganizationIdAndRole(organizationId, role);
        } else if (organizationId != null && enabled != null) {
            users = userRepository.findByOrganizationIdAndEnabled(organizationId, enabled);
        } else if (organizationId != null) {
            users = userRepository.findByOrganizationId(organizationId);
        } else if (role != null) {
            users = userRepository.findByRole(role);
        } else if (enabled != null) {
            users = userRepository.findByEnabled(enabled);
        } else if (search != null && !search.isEmpty()) {
            users = userRepository.search(search);
        } else {
            return userRepository.findAll(pageable);
        }
        
        return new PageImpl<>(users, pageable, users.size());
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByOrganization(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public User getCurrentUserProfile() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return getUserById(principal.getId());
    }

    public User createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        mapRequestToUser(request, user);
        
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            user.setOrganization(organization);
        }
        
        String currentUser = getCurrentUsername();
        user.setCreatedBy(currentUser);
        user.setLastModifiedBy(currentUser);

        return userRepository.save(user);
    }

    public User updateUser(UUID id, UserRequest request) {
        User user = getUserById(id);
        UserPrincipal currentUser = getCurrentUserPrincipal();
        
        // Check if user is updating their own profile or has admin role
        if (!currentUser.getId().equals(id) && !currentUser.getRole().equals("ADMIN")) {
            throw new RuntimeException("Insufficient permissions to update this user");
        }

        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        mapRequestToUser(request, user);
        
        // Only admin can change password during update
        if (request.getPassword() != null && currentUser.getRole().equals("ADMIN")) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Only admin can change organization
        if (request.getOrganizationId() != null && currentUser.getRole().equals("ADMIN")) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            user.setOrganization(organization);
        }
        
        user.setLastModifiedBy(getCurrentUsername());

        return userRepository.save(user);
    }

    public User updateCurrentUserProfile(UserRequest request) {
        UserPrincipal principal = getCurrentUserPrincipal();
        User user = getUserById(principal.getId());

        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setType(request.getType());
        user.setLastModifiedBy(getCurrentUsername());

        return userRepository.save(user);
    }

    public void changePassword(UUID id, PasswordChangeRequest request) {
        User user = getUserById(id);
        UserPrincipal currentUser = getCurrentUserPrincipal();
        
        // Check if user is changing their own password or has admin role
        if (!currentUser.getId().equals(id) && !currentUser.getRole().equals("ADMIN")) {
            throw new RuntimeException("Insufficient permissions to change password for this user");
        }
        
        // Verify current password if user is changing their own password
        if (currentUser.getId().equals(id) && 
            !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastModifiedBy(getCurrentUsername());
        userRepository.save(user);
    }

    public User enableUser(UUID id) {
        User user = getUserById(id);
        user.setEnabled(true);
        user.setStatus("ACTIVE");
        user.setLastModifiedBy(getCurrentUsername());
        return userRepository.save(user);
    }

    public User disableUser(UUID id) {
        User user = getUserById(id);
        user.setEnabled(false);
        user.setStatus("INACTIVE");
        user.setLastModifiedBy(getCurrentUsername());
        return userRepository.save(user);
    }

    public User updateRole(UUID id, String role) {
        User user = getUserById(id);
        user.setRole(role);
        user.setLastModifiedBy(getCurrentUsername());
        return userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    private void mapRequestToUser(UserRequest request, User user) {
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        user.setRole(request.getRole() != null ? request.getRole() : "USER");
        user.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        user.setType(request.getType());
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return ((UserPrincipal) principal).getUsername();
        }
        return "system";
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        throw new RuntimeException("Unable to get current user");
    }
}