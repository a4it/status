# Section 14: Contributing & Governance

---

## 172. Code Style Guide

### Java Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Entities (models) | PascalCase noun | `StatusPlatform`, `LogApiKey` |
| Services | PascalCase + `Service` suffix | `StatusPlatformService` |
| Repositories | PascalCase + `Repository` suffix | `StatusPlatformRepository` |
| REST Controllers | PascalCase + `Controller` suffix | `StatusPlatformController` |
| MVC Controllers | PascalCase + `Controller` suffix | `AdminController` |
| Request classes | PascalCase + `Request` suffix | `StatusPlatformRequest` |
| Response classes | PascalCase + `Response` suffix | `StatusPlatformResponse` |
| Scheduler classes | PascalCase + `Scheduler` suffix | `HealthCheckScheduler` |
| Config classes | PascalCase + `Config` suffix | `SecurityConfig` |
| Package: entities | `models` | `org.automatize.status.models` |
| Package: REST APIs | `controllers.api` | `org.automatize.status.controllers.api` |
| Package: API types | `api.request` / `api.response` | |

### JavaScript Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Page JS files | `kebab-case/kebab-case.js` | `status-platforms/status-platforms.js` |
| Functions | `camelCase` | `loadPlatforms()`, `handleCreateSubmit()` |
| Constants | `UPPER_SNAKE_CASE` | `API_BASE_URL`, `DEFAULT_PAGE_SIZE` |
| DOM IDs | `kebab-case` | `platform-table`, `create-modal` |
| localStorage keys | `{username}:{table}:{stateType}` | `admin:platforms:columns` |

### Template Naming Conventions

| Template type | Location | Example |
|--------------|----------|---------|
| Admin pages | `templates/admin/` | `admin/platforms.html` |
| Auth pages | `templates/authentication/` | `authentication/login.html` |
| Public pages | `templates/public/` | `public/status.html` |
| Shared fragments | `templates/fragments/` | `fragments/sidebar.html` |

---

## 173. Adding a New Entity — Checklist

When adding a new entity, follow this order:

1. **Write the Flyway migration**
   - File: `src/main/resources/db/migration/V{next}__create_{entity_name}.sql`
   - Include all columns, constraints, indexes, FK references
   - Test locally before writing application code

2. **Create the entity class**
   - File: `src/main/java/org/automatize/status/models/{EntityName}.java`
   - Add `@Entity`, `@Table`, `@Id`, all field annotations
   - Add audit columns: `createdBy`, `createdDate`, `lastModifiedBy`, `lastModifiedDate`
   - Add relationships with appropriate cascade and fetch type

3. **Create the repository**
   - File: `src/main/java/org/automatize/status/repositories/{EntityName}Repository.java`
   - Extend `JpaRepository<EntityName, UUID>`
   - Add custom query methods as needed

4. **Create Request and Response classes**
   - `src/main/java/org/automatize/status/api/request/{EntityName}Request.java`
   - `src/main/java/org/automatize/status/api/response/{EntityName}Response.java`
   - Add validation annotations (`@NotBlank`, `@NotNull`, `@Size`) on Request fields

5. **Create the service**
   - File: `src/main/java/org/automatize/status/services/{EntityName}Service.java`
   - Implement CRUD methods: `findAll`, `findById`, `create`, `update`, `delete`
   - Add `@PreAuthorize` where role restrictions apply
   - Scope all queries to the user's organisation

6. **Create the REST controller**
   - File: `src/main/java/org/automatize/status/controllers/api/{EntityName}Controller.java`
   - `@RestController`, `@RequestMapping("/api/{entity-names}")`
   - Map all CRUD endpoints to service methods
   - Use `@AuthenticationPrincipal UserPrincipal` to get current user

7. **Add the MVC route** (if admin UI is needed)
   - Add a `GET /admin/{entity-names}` method to `AdminController`
   - Return the template name with `activeNav` set

8. **Create the Thymeleaf template**
   - File: `src/main/resources/templates/admin/{entity-names}.html`
   - Include sidebar, navbar fragments
   - Add the data table container and create/edit modal structure

9. **Create the JavaScript file**
   - File: `src/main/resources/static/js/admin/{entity-names}/{entity-names}.js`
   - Load data from REST API, render table, implement create/edit/delete modal
   - Persist column and filter state to localStorage

10. **Add the nav link to the sidebar fragment**
    - Add an `<a>` link in `templates/fragments/sidebar.html`

---

## 174. Adding a New API Endpoint

Follow this pattern when adding an endpoint to an existing controller:

1. **Create or update the Request class** in `api/request/`
   - One Request class per operation if inputs differ significantly
   - Use validation annotations

2. **Create or update the Response class** in `api/response/`
   - Return only what the client needs — do not expose sensitive fields

3. **Add the service method**
   - Place business logic here, not in the controller
   - Use `@PreAuthorize` for role-based restrictions

4. **Add the controller method**
   - Map to `GET/POST/PUT/PATCH/DELETE` with a clear path
   - Annotate with `@Operation` (SpringDoc) for OpenAPI docs
   - Return appropriate HTTP status: `200`, `201`, `204`, `400`, `404`

5. **Add the security annotation** if the endpoint requires specific roles:
   ```java
   @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
   ```

6. **If the endpoint is public**, add the URL pattern to `SecurityConfig.filterChain()` as `permitAll()`

7. **Test manually** via the REST API (Postman, curl) and verify the OpenAPI spec at `/v3/api-docs`

---

## 175. Adding a New Admin Page

1. **Add the MVC route to `AdminController`:**
   ```java
   @GetMapping("/admin/new-feature")
   public String newFeature(Model model) {
       model.addAttribute("applicationName", applicationName);
       model.addAttribute("buildNumber", buildNumber);
       model.addAttribute("copyright", copyright);
       model.addAttribute("activeNav", "new-feature");
       return "admin/new-feature";
   }
   ```

2. **Create the Thymeleaf template** at `templates/admin/new-feature.html`:
   - Copy the structure from an existing admin template
   - Update the page title and `activeNav`-dependent class on the sidebar link
   - Add a container div where the JS will render the table/content

3. **Create the JavaScript file** at `static/js/admin/new-feature/new-feature.js`:
   - Follow the standard pattern from item 121
   - Auth check, load data, render table, wire up actions

4. **Add the sidebar nav link** in `templates/fragments/sidebar.html`:
   ```html
   <a th:href="@{/admin/new-feature}"
      th:classappend="${activeNav == 'new-feature'} ? 'active' : ''"
      class="nav-link">
       <i class="ti ti-your-icon nav-link-icon"></i>
       <span class="nav-link-title">New Feature</span>
   </a>
   ```

5. **Add the REST API endpoint** (see item 174 — controller, service, request/response classes)

6. **Test** by navigating to `/admin/new-feature` and verifying the JS loads data correctly

---

## 176. Dependency Update Policy

1. **Check for updates** quarterly via:
   ```bash
   mvn versions:display-dependency-updates
   mvn versions:display-plugin-updates
   ```

2. **Evaluate the update:**
   - Read the changelog / release notes
   - Check for breaking changes
   - Check Spring Boot's dependency management — many dependencies are managed via the BOM and should be updated by upgrading Spring Boot, not individually

3. **Update and test:**
   - Update the version in `pom.xml`
   - Run `mvn clean test` — all tests must pass
   - Perform a manual smoke test of key features

4. **Security-critical updates:** Apply immediately regardless of schedule. Subscribe to GitHub Dependabot alerts or OWASP Dependency-Check reports.

5. **Major version updates** (e.g., Spring Boot 3 → 4): Treat as a migration project, not a simple update. Plan for breaking changes and allocate testing time accordingly.

---

## 177. Security Vulnerability Disclosure Process

1. **Do not create a public GitHub issue** for security vulnerabilities.

2. **Contact the maintainer directly** via email (see project metadata in `pom.xml` for contact details).

3. **Include in your report:**
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (optional)

4. **Response commitment:** Acknowledge within 48 hours, patch within 14 days for critical issues.

5. **After patching:** A new version will be tagged and released. The vulnerability will be documented in the release notes after the fix is deployed.

---

## 178. API Versioning Strategy

**Current state:** API v1 is implicitly embedded in the URL structure (`/api/...`). No explicit version prefix is used.

**When a v2 would be introduced:**
- A breaking change is required that cannot be made backward-compatible
- Multiple consumers exist who cannot be migrated simultaneously

**How to introduce v2:**
1. Add a new URL prefix: `/api/v2/...`
2. Create new controllers under `controllers/api/v2/`
3. Keep `v1` controllers running in parallel until all consumers migrate
4. Set a deprecation timeline — announce via API response headers: `Deprecation: true`, `Sunset: <date>`
5. After the sunset date, remove v1 controllers

**Rule:** Never introduce breaking changes without a versioned migration path. A breaking change is:
- Removing a field from a response
- Changing a field type or format
- Changing required/optional semantics
- Removing an endpoint

Non-breaking changes (can be made without a version bump):
- Adding new optional fields to responses
- Adding new optional request parameters
- Adding new endpoints
