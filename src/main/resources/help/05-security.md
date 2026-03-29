# Section 5: Security

---

## 57. Security Architecture Overview

The application uses **stateless JWT-based authentication**:

- No server-side sessions — every request must carry a token
- Tokens are signed with HMAC-SHA256 using a shared secret
- Passwords are hashed with BCrypt before storage
- Role-based access control (RBAC) is enforced at both the URL level (SecurityFilterChain) and method level (`@PreAuthorize`)

**Key components:**

| Class | Role |
|-------|------|
| `SecurityConfig` | Configures the filter chain, CORS, URL auth rules |
| `JwtUtils` | Generates, validates, and parses JWT tokens |
| `JwtAuthenticationFilter` | Extracts and validates the Bearer token on every request |
| `JwtAuthenticationEntryPoint` | Returns 401 for unauthenticated requests |
| `CustomUserDetailsService` | Loads `UserPrincipal` from DB for Spring Security |
| `UserPrincipal` | The authenticated principal object available in controllers |

---

## 58. JWT Token Structure

Tokens use the **HS256 algorithm** (HMAC-SHA256).

### Access Token Claims

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | String | Username |
| `iat` | Long | Issued-at timestamp (epoch seconds) |
| `exp` | Long | Expiration timestamp (epoch seconds) |
| `userId` | String (UUID) | User's database UUID |
| `email` | String | User's email address |
| `organizationId` | String (UUID) | User's organisation UUID (null for SUPERADMIN before context selection) |
| `role` | String | User's role: `ADMIN`, `MANAGER`, `USER`, or `SUPERADMIN` |
| `tenantId` | String (UUID) | Selected tenant UUID (only present after SUPERADMIN context selection) |
| `requiresContextSelection` | Boolean | `true` for SUPERADMIN until they select a tenant/org context |

### Refresh Token Claims

Refresh tokens contain only:
- `sub` — username
- `iat` / `exp` — timestamps

They do not carry roles or organisation context, so they cannot be used directly for API access.

---

## 59. Token Lifecycle

```
Login (POST /api/auth/login)
  │
  ├── Returns: accessToken (24h), refreshToken (7d)
  │
  │   [Normal operation]
  │   Client includes: Authorization: Bearer <accessToken>
  │
  │   [Access token expires after 24h]
  ▼
Refresh (POST /api/auth/refresh)
  │
  ├── Request body: { "refreshToken": "<refreshToken>" }
  ├── Returns: new accessToken (24h), new refreshToken (7d)
  │
  │   [Refresh token expires after 7d]
  ▼
Re-authenticate (POST /api/auth/login)

Logout:
  - Client-side: discard both tokens from localStorage/cookie
  - No server-side token revocation (stateless design)
  - To force logout: rotate jwt.secret (invalidates all existing tokens)
```

---

## 60. `JwtUtils` Reference

**File:** `src/main/java/org/automatize/status/security/JwtUtils.java`

| Method | Description |
|--------|-------------|
| `generateJwtToken(Authentication)` | Creates access token from Spring Security `Authentication` object; extracts claims from `UserPrincipal` |
| `generateJwtTokenFromUserId(UUID, String, String, UUID, String)` | Creates access token from explicit user attributes; used during token refresh |
| `generateJwtTokenWithContext(UUID, String, String, UUID, String, UUID)` | Creates access token with `tenantId` claim set; used after SUPERADMIN selects context |
| `generateRefreshToken(String username)` | Creates a refresh token with only the username claim |
| `validateJwtToken(String)` | Returns `true` if token signature is valid and not expired |
| `getUserNameFromJwtToken(String)` | Extracts `sub` claim (username) |
| `getUserIdFromJwtToken(String)` | Extracts `userId` claim as `UUID` |
| `getTenantIdFromJwtToken(String)` | Extracts `tenantId` claim as `UUID` (null if not set) |
| `getOrganizationIdFromJwtToken(String)` | Extracts `organizationId` claim as `UUID` (null if not set) |
| `requiresContextSelection(String)` | Returns `true` if `requiresContextSelection` claim is `true` |
| `getAllClaimsFromToken(String)` | Returns the full `Claims` object for arbitrary claim access |

**Signing algorithm:** HS256 using the Base64-decoded `jwt.secret` property as the HMAC key.

---

## 61. `JwtAuthenticationFilter`

**File:** `src/main/java/org/automatize/status/security/JwtAuthenticationFilter.java`

This filter runs **before** `UsernamePasswordAuthenticationFilter` on every request.

**Processing flow:**
1. Extract the `Authorization` header
2. Check for `Bearer ` prefix; skip if absent (unauthenticated request)
3. Extract the token string after `Bearer `
4. Call `jwtUtils.validateJwtToken(token)` — skip if invalid
5. Extract the username via `jwtUtils.getUserNameFromJwtToken(token)`
6. Load `UserDetails` via `customUserDetailsService.loadUserByUsername(username)`
7. Create `UsernamePasswordAuthenticationToken` with the `UserPrincipal`
8. Set the `SecurityContextHolder` authentication

If validation fails at any step, the filter does nothing (does not throw) — the request proceeds unauthenticated and will be rejected by the `SecurityFilterChain` authorization rules if the endpoint requires auth.

---

## 62. `JwtAuthenticationEntryPoint`

**File:** `src/main/java/org/automatize/status/security/JwtAuthenticationEntryPoint.java`

Called when a request reaches an authenticated endpoint without a valid token.

**Response:**
- HTTP Status: `401 Unauthorized`
- Content-Type: `application/json`
- Body: `{"error": "Unauthorized", "message": "..."}`

This ensures REST API clients receive a proper JSON error rather than an HTML redirect.

---

## 63. `CustomUserDetailsService`

**File:** `src/main/java/org/automatize/status/security/CustomUserDetailsService.java`

Implements Spring Security's `UserDetailsService` interface.

`loadUserByUsername(String username)`:
1. Queries `UserRepository.findByUsername(username)`
2. Throws `UsernameNotFoundException` if not found
3. Returns a `UserPrincipal` wrapping the `User` entity

Used by both:
- The `JwtAuthenticationFilter` (on every authenticated request)
- The `DaoAuthenticationProvider` (on login to verify password)

---

## 64. `UserPrincipal`

**File:** `src/main/java/org/automatize/status/security/UserPrincipal.java`

Implements Spring Security's `UserDetails`. Wraps the `User` entity and exposes:

| Method / Field | Returns | Description |
|----------------|---------|-------------|
| `getId()` | UUID | User's database ID |
| `getUsername()` | String | Login username |
| `getEmail()` | String | Email address |
| `getRole()` | String | Single role string |
| `getOrganizationId()` | UUID | Owning organisation (null for SUPERADMIN) |
| `getAuthorities()` | Collection | `SimpleGrantedAuthority("ROLE_" + role)` |
| `isEnabled()` | Boolean | `user.enabled` |
| `isAccountNonExpired()` | Boolean | Always `true` |
| `isAccountNonLocked()` | Boolean | Always `true` |
| `isCredentialsNonExpired()` | Boolean | Always `true` |

In controllers, access the principal via:
```java
@AuthenticationPrincipal UserPrincipal currentUser
```

---

## 65. CORS Configuration

**Configured in:** `SecurityConfig.corsConfigurationSource()`

| Setting | Current value | Production recommendation |
|---------|--------------|--------------------------|
| `allowedOrigins` | `["*"]` (all origins) | Lock to specific domains: `["https://yourdomain.com"]` |
| `allowedMethods` | `GET, POST, PUT, PATCH, DELETE, OPTIONS` | Keep as-is |
| `allowedHeaders` | `["*"]` (all headers) | Can restrict to needed headers |
| `exposedHeaders` | `["Authorization"]` | Required for clients to read the `Authorization` header |

**Production hardening:** Replace `setAllowedOrigins(Arrays.asList("*"))` with the actual production domain(s). Using `*` in production allows any website to make API requests on behalf of authenticated users.

---

## 66. CSRF Policy

**CSRF is disabled** via `csrf.disable()` in `SecurityConfig`.

**Why:** The application uses stateless JWT authentication. CSRF attacks target session cookie-based authentication where the browser automatically includes credentials. Since JWT tokens are stored in `localStorage` and explicitly added to each request via the `Authorization` header, the browser will not automatically send them cross-origin, making CSRF attacks ineffective.

**When to re-enable CSRF:** If you ever switch to session-cookie-based authentication (e.g., adding `SessionCreationPolicy.IF_REQUIRED`), re-enable CSRF protection and configure the appropriate `CsrfTokenRepository`.

---

## 67. Role-Based Access Control Matrix

| Resource | SUPERADMIN | ADMIN | MANAGER | USER |
|----------|-----------|-------|---------|------|
| Tenant management | Full CRUD | — | — | — |
| Organisation management | Full CRUD | Full CRUD | Read | Read |
| User management | Full CRUD | Full CRUD | Read | Read own profile |
| Platform management | Full CRUD | Full CRUD | Full CRUD | Read |
| App management | Full CRUD | Full CRUD | Full CRUD | Read |
| Incident management | Full CRUD | Full CRUD | Create/Update | Read |
| Maintenance management | Full CRUD | Full CRUD | Create/Update | Read |
| Log ingestion (API key) | — | — | — | External systems |
| Log viewing | All tenants | Own org | Own org | — |
| Alert/Drop rules | Full CRUD | Full CRUD | Full CRUD | Read |
| Health check config | Full CRUD | Full CRUD | Full CRUD | Read |

---

## 68. Method-Level Security

`@EnableMethodSecurity(prePostEnabled = true)` is set in `SecurityConfig`, enabling:

```java
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
public void adminOnlyOperation() { ... }

@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or hasRole('SUPERADMIN')")
public void managerAndAboveOperation() { ... }
```

Method-level security is applied in the service or controller layer where fine-grained access control is needed beyond URL-level rules.

---

## 69. Public Endpoints List

The following endpoints are accessible **without authentication**:

| Pattern | Why public |
|---------|-----------|
| `/api/auth/**` | Login, register, refresh |
| `/api/public/**` | Public status page data |
| `/api/events/log` | External event posting via API key |
| `/api/logs`, `/api/logs/batch` | Log ingestion via Log API key |
| `/`, `/login`, `/logout`, `/register`, `/forgot-password` | Auth forms |
| `/admin/**` | Admin UI shell (actual data fetched via authenticated API calls) |
| `/incidents/**`, `/maintenance`, `/history` | Public status pages |
| `/static/**`, `/css/**`, `/js/**`, `/fonts/**` | Static assets |
| `/swagger/**`, `/v3/api-docs/**` | API documentation |
| `/actuator/**` | Health endpoints |

**Note:** `/admin/**` is `permitAll` because authentication is enforced client-side via JWT. The Thymeleaf template is a shell; all sensitive data requires an authenticated API call.

---

## 70. Log API Key Authentication

External services that POST logs to `/api/logs` authenticate using a Log API key in the request header:

```
X-Api-Key: <plain-key>
```

**Validation flow:**
1. Receive `X-Api-Key` header
2. Look up enabled `LogApiKey` records for the organisation
3. Compare the provided key against stored hashes
4. Reject if no match found or key is disabled
5. Update `lastUsedAt` on the matched key

Log API keys are scoped per organisation. Generate keys via the Admin UI → Log API Keys page.

---

## 71. Platform Event API Key

Platform events (`POST /api/events/log`) use a per-component or per-app API key:

```
X-Api-Key: <plain-event-key>
```

These keys are generated per-platform or per-component in the admin panel. The validation follows the same hash-comparison pattern as Log API keys. A separate key type is used so log ingestion keys and event posting keys are independent.

---

## 72. Password Policy

| Requirement | Current configuration |
|-------------|----------------------|
| Minimum length | Enforced by validation annotation on `UserRequest` |
| Complexity | Not enforced beyond length (can be added via custom validator) |
| Hashing algorithm | BCrypt with cost factor 10 (default) |
| Storage | Only the BCrypt hash is stored — plain text is never persisted or logged |
| Reset | Password reset flow uses a one-time token sent via email |

To change the BCrypt cost factor, modify the `BCryptPasswordEncoder()` constructor in `SecurityConfig.passwordEncoder()`.

---

## 73. Security Hardening Checklist for Production

- [ ] **Replace JWT secret:** Generate a strong random secret: `openssl rand -base64 32`
- [ ] **Lock CORS origins:** Change `allowedOrigins(["*"])` to your domain in `SecurityConfig`
- [ ] **Disable Swagger UI:** Confirm `springdoc.swagger-ui.enabled=false` in prod config
- [ ] **Disable data initializer:** Set `data.initializer.enabled=false` after first setup
- [ ] **Change default passwords:** `admin`/`admin` and `superadmin`/`superadmin` must be changed immediately
- [ ] **Enable HTTPS:** Configure an SSL certificate (reverse proxy with nginx/Caddy recommended)
- [ ] **Production logging:** Set `logging.level.org.springframework.security=WARN` to avoid leaking auth details
- [ ] **Set security headers:** Add `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options` via Spring Security headers config or reverse proxy
- [ ] **Review public endpoint list:** Ensure `/admin/**` being `permitAll` is acceptable for your threat model
- [ ] **Rotate secrets:** Establish a process for rotating `jwt.secret` and database passwords periodically
