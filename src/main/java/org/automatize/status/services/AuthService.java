package org.automatize.status.services;

import org.automatize.status.api.request.LoginRequest;
import org.automatize.status.api.request.RefreshTokenRequest;
import org.automatize.status.api.request.RegisterRequest;
import org.automatize.status.api.response.AuthResponse;
import org.automatize.status.api.response.MessageResponse;
import org.automatize.status.models.Organization;
import org.automatize.status.models.User;
import org.automatize.status.repositories.OrganizationRepository;
import org.automatize.status.repositories.UserRepository;
import org.automatize.status.security.JwtUtils;
import org.automatize.status.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String refreshToken = jwtUtils.generateRefreshToken(userPrincipal.getUsername());
        
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400L); // 24 hours
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setOrganizationId(user.getOrganization() != null ? user.getOrganization().getId() : null);

        return response;
    }

    public MessageResponse registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return new MessageResponse("Username is already taken!", false);
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new MessageResponse("Email is already in use!", false);
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFullName(registerRequest.getFullName());
        user.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : "USER");
        user.setEnabled(true);
        user.setStatus("ACTIVE");
        user.setCreatedBy(registerRequest.getUsername());
        user.setLastModifiedBy(registerRequest.getUsername());

        if (registerRequest.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(registerRequest.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            user.setOrganization(organization);
        }

        userRepository.save(user);

        return new MessageResponse("User registered successfully!", true);
    }

    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new RuntimeException("Refresh token mismatch");
        }

        String newAccessToken = jwtUtils.generateJwtTokenFromUserId(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getRole()
        );

        AuthResponse response = new AuthResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400L); // 24 hours
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setOrganizationId(user.getOrganization() != null ? user.getOrganization().getId() : null);

        return response;
    }

    public MessageResponse logout(String token) {
        try {
            String jwt = token.substring(7); // Remove "Bearer " prefix
            UUID userId = jwtUtils.getUserIdFromJwtToken(jwt);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setRefreshToken(null);
            userRepository.save(user);

            SecurityContextHolder.clearContext();
            
            return new MessageResponse("Logged out successfully", true);
        } catch (Exception e) {
            logger.error("Error during logout", e);
            return new MessageResponse("Error during logout", false);
        }
    }

    public AuthResponse getCurrentUser(String token) {
        String jwt = token.substring(7); // Remove "Bearer " prefix
        UUID userId = jwtUtils.getUserIdFromJwtToken(jwt);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setOrganizationId(user.getOrganization() != null ? user.getOrganization().getId() : null);

        return response;
    }
}