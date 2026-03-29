# Section 9: MVC Controllers & Templates

---

## 136. `AdminController`

**File:** `src/main/java/org/automatize/status/controllers/AdminController.java`

**Route table:**

| HTTP | Path | Method | Template | Active Nav |
|------|------|--------|----------|-----------|
| GET | `/admin` | `dashboard()` | `admin/dashboard` | `dashboard` |
| GET | `/admin/platforms` | `platforms()` | `admin/platforms` | `platforms` |
| GET | `/admin/issues` | `issues()` | `admin/issues` | `issues` |
| GET | `/admin/components` | `components()` | `admin/components` | `components` |
| GET | `/admin/subscribers` | `subscribers()` | `admin/subscribers` | `subscribers` |
| GET | `/admin/events` | `events()` | `admin/events` | `events` |
| GET | `/admin/health-checks` | `healthChecks()` | `admin/health-checks` | `health-checks` |
| GET | `/admin/tenants` | `tenants()` | `admin/tenants` | `tenants` |
| GET | `/admin/organizations` | `organizations()` | `admin/organizations` | `organizations` |
| GET | `/admin/logs` | `logs()` | `admin/logs` | `logs` |
| GET | `/admin/drop-rules` | `dropRules()` | `admin/drop-rules` | `drop-rules` |
| GET | `/admin/log-metrics` | `logMetrics()` | `admin/log-metrics` | `log-metrics` |
| GET | `/admin/alert-rules` | `alertRules()` | `admin/alert-rules` | `alert-rules` |
| GET | `/admin/log-api-keys` | `logApiKeys()` | `admin/log-api-keys` | `log-api-keys` |
| GET | `/admin/users` | `users()` | `admin/users` | `users` |
| GET | `/admin/process-mining` | `processMining()` | `admin/process-mining` | `process-mining` |
| GET | `/admin/select-context` | `selectContext()` | `admin/select-context` | `select-context` |
| GET | `/admin/login` | `login()` | `admin/login` | — |

**Model attributes added to every admin route:**

| Attribute | Source | Description |
|-----------|--------|-------------|
| `applicationName` | `application.properties` | App name for page title |
| `buildNumber` | `build.properties` | Current build version |
| `buildDate` | `application.properties` (Maven filtered) | Build timestamp |
| `copyright` | `application.properties` | Copyright notice for footer |
| `activeNav` | Hardcoded per route | Highlights the active nav item in the sidebar |

**Auth guards:** The controller itself does not enforce authentication — it passes. Authentication is enforced client-side: if `auth.isAuthenticated()` returns false in the page's JavaScript, the page redirects to `/login`. This is acceptable because the template contains no sensitive data; all data requires an authenticated API call.

---

## 137. `AuthenticationController`

**File:** `src/main/java/org/automatize/status/controllers/AuthenticationController.java`

**Routes:**

| HTTP | Path | Template | Description |
|------|------|----------|-------------|
| GET | `/login` | `authentication/login` | Login form |
| GET | `/register` | `authentication/register` | Registration form |
| GET | `/forgot-password` | `authentication/forgot-password` | Forgot password form |
| GET/POST | `/logout` | redirect to `/login` | Clears server-side state (if any) |

**Flash message usage:**
- Successful registration: `redirectAttributes.addFlashAttribute("successMessage", "Account created. Please log in.")`
- Flash messages are rendered by the template as Tabler-styled alerts
- Flash attributes are added to the `RedirectAttributes` parameter on redirect routes

**Model attributes:**
- `registrationEnabled` — from `app.registration.enabled` property — controls whether the register link is shown on the login page

---

## 138. `PublicController`

**File:** `src/main/java/org/automatize/status/controllers/PublicController.java`

**Routes:**

| HTTP | Path | Template | Description |
|------|------|----------|-------------|
| GET | `/` or `/{slug}` | `public/status` | Public status page for an app |
| GET | `/incidents` | `public/incidents` | Public incident list |
| GET | `/incidents/{id}` | `public/incident-detail` | Incident detail with timeline |
| GET | `/maintenance` | `public/maintenance` | Maintenance schedule |
| GET | `/history` | `public/history` | Historical uptime |

**Slug resolution:** The `/{slug}` path is matched against `StatusApp.slug`. If not found, returns a 404 template.

**Model attributes:** Only the `slug` is passed to the template — all data is fetched by `status.js` via public API endpoints.

---

## 139. Thymeleaf Layout Conventions

**Shared layout:** All admin templates extend a common layout defined in the `<html>` tag using Thymeleaf Layout Dialect or a manual `th:replace` fragment approach.

**Fragment structure:**
- `templates/fragments/head.html` — `<head>` with CSS links and meta tags
- `templates/fragments/sidebar.html` — navigation sidebar with active nav highlighting
- `templates/fragments/navbar.html` — top navigation bar with user info
- `templates/fragments/scripts.html` — common JS vendor includes

**Active nav highlighting:** Each template sets `activeNav` in the controller. The sidebar fragment uses:
```html
<a th:classappend="${activeNav == 'platforms'} ? 'active' : ''" href="/admin/platforms">Platforms</a>
```

**CSS and JS references:** Templates reference local files only:
```html
<link rel="stylesheet" th:href="@{/css/tabler.min.css}">
<script th:src="@{/js/vendor/tabler.min.js}"></script>
```

---

## 140. Flash Message Usage

Spring's `RedirectAttributes` is used for messages that survive a redirect:

**In a controller (on redirect):**
```java
redirectAttributes.addFlashAttribute("successMessage", "Platform deleted successfully.");
return "redirect:/admin/platforms";
```

**In a template:**
```html
<div th:if="${successMessage}" class="alert alert-success">
    <span th:text="${successMessage}"></span>
</div>
<div th:if="${errorMessage}" class="alert alert-danger">
    <span th:text="${errorMessage}"></span>
</div>
```

**Message types:**

| Attribute | Style | Use case |
|-----------|-------|---------|
| `successMessage` | `alert-success` (green) | Successful form submission |
| `errorMessage` | `alert-danger` (red) | Validation or system error |
| `warningMessage` | `alert-warning` (yellow) | Advisory warning |
| `infoMessage` | `alert-info` (blue) | Informational notice |

Flash messages are one-time — they disappear after the next request.

---

## 141. Template Variable Reference

Variables passed to each template by the `AdminController`:

| Variable | Type | All templates | Only specific templates |
|----------|------|--------------|------------------------|
| `applicationName` | String | Yes | — |
| `buildNumber` | String | Yes | — |
| `buildDate` | String | Yes | — |
| `copyright` | String | Yes | — |
| `activeNav` | String | Yes | — |
| `registrationEnabled` | Boolean | — | `authentication/login` |
| `slug` | String | — | `public/status` |

**Rule:** MVC controllers must only pass these configuration values. Any actual data (lists of platforms, user details, etc.) must be fetched by JavaScript via REST API calls.
