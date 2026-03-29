# Section 8: Frontend Architecture

---

## 117. Frontend Overview

The frontend uses a **Thymeleaf + REST API split**:

| Layer | What it does |
|-------|-------------|
| **Thymeleaf templates** | Render the HTML shell; receive only config values from the MVC controller (app name, build info, active nav) |
| **JavaScript (ES6+)** | Fetch all data from REST APIs; render dynamic content; handle user interactions |
| **Bootstrap 5 / Tabler.io** | UI components, layout, icons |

**What the server renders:** The page skeleton, navigation, and any configuration values from `application.properties`.

**What JavaScript fetches:** All entity data (platforms, apps, logs, metrics, incidents, etc.) via authenticated REST API calls.

This split means templates are simple and stateless — the same HTML shell works for any user because all personalised data is loaded dynamically.

---

## 118. `shared/api.js`

**File:** `src/main/resources/static/js/shared/api.js`

The central HTTP client used by all page JS files.

**Responsibilities:**
- Base URL configuration (auto-detected from `window.location.origin`)
- Injecting the `Authorization: Bearer <token>` header on every request
- Centralised error handling — redirects to `/login` on 401
- JSON serialisation/deserialisation
- Exposing wrapper methods: `get()`, `post()`, `put()`, `patch()`, `delete()`

**Usage pattern:**
```javascript
// GET with pagination
const data = await api.get('/api/status-platforms?page=0&size=20');

// POST with body
const result = await api.post('/api/status-platforms', { name: 'My Platform' });

// DELETE
await api.delete(`/api/status-platforms/${id}`);
```

**Error handling:** All methods return a resolved promise with the response data on success, or throw an error with the API error message on failure. The calling code wraps calls in `try/catch` and shows notifications.

---

## 119. `shared/auth.js`

**File:** `src/main/resources/static/js/shared/auth.js`

Manages token storage and authentication state.

**Token storage:**
- Access token: `localStorage.getItem('accessToken')`
- Refresh token: `localStorage.getItem('refreshToken')`
- User info: `localStorage.getItem('user')` — JSON object with `{username, email, role, organizationId}`

**Key functions:**

| Function | Description |
|----------|-------------|
| `getToken()` | Returns current access token |
| `setTokens(accessToken, refreshToken)` | Persists tokens to localStorage |
| `clearTokens()` | Removes all auth data from localStorage |
| `logout()` | Calls clearTokens() and redirects to `/login` |
| `isAuthenticated()` | Returns `true` if an access token exists |
| `getCurrentUser()` | Returns parsed user object from localStorage |
| `refreshToken()` | Calls `/api/auth/refresh` and updates stored tokens |

**Token refresh:** `api.js` calls `auth.refreshToken()` automatically when a 401 is received (token expired). If the refresh also fails, the user is redirected to login.

---

## 120. `shared/notifications.js`

**File:** `src/main/resources/static/js/shared/notifications.js`

Toast/alert notification system following Tabler.io styling.

**Functions:**

| Function | Description |
|----------|-------------|
| `showSuccess(message)` | Green success toast |
| `showError(message)` | Red error toast |
| `showWarning(message)` | Yellow warning toast |
| `showInfo(message)` | Blue info toast |
| `showConfirm(message, onConfirm)` | Modal confirmation dialog |

**Auto-dismiss:** Success and info toasts auto-dismiss after 4 seconds. Error and warning toasts remain until dismissed by the user.

**Accessibility:** All toasts include `role="alert"` and `aria-live="polite"` for screen readers.

---

## 121. Page JS File Convention

Each admin page has its own dedicated JavaScript file:

```
/static/js/admin/{pagename}/{pagename}.js
```

**What each page JS file must do:**
1. Import (or inline reference) `api.js`, `auth.js`, `notifications.js`
2. Check authentication on page load — redirect to login if not authenticated
3. Load initial data from the relevant REST API endpoint(s)
4. Render data into the page's table or form
5. Wire up event listeners for create/edit/delete actions
6. Implement the modal form for create and edit operations
7. Implement column state persistence via localStorage (per user, per table)

**Standard page init pattern:**
```javascript
document.addEventListener('DOMContentLoaded', async () => {
    if (!auth.isAuthenticated()) {
        window.location.href = '/login';
        return;
    }
    await loadData();
    setupEventListeners();
});
```

---

## 122. Admin Dashboard (`dashboard.js`)

**File:** `static/js/admin/dashboard/dashboard.js`

**Widgets loaded:**
- Platform count and status summary
- Active incidents count
- Recent health check failures
- Log ingestion rate (last 24h)

**Data sources:**
- `GET /api/status-platforms` (count)
- `GET /api/incidents` (active count)
- `GET /api/log-metrics` (recent rate)

**Refresh behaviour:** Data is loaded once on page load. No auto-refresh (can be added by wrapping `loadData()` in `setInterval`).

---

## 123. Platforms Page (`platforms.js`)

**File:** `static/js/admin/platforms/platforms.js`

**Table columns:** Name, Organisation, Status, App Count, Created Date, Actions (Edit, Delete)

**Filters:** Organisation filter (SUPERADMIN only), Status filter

**Create/Edit modal fields:**
- Name (required)
- Description (optional)
- Organisation selector (SUPERADMIN only)

**Row click:** Navigates to the apps page filtered by platform.

---

## 124. Components Page (`components.js`)

**File:** `static/js/admin/components/components.js`

**Drag-to-reorder UX:**
- Uses HTML5 drag-and-drop on table rows
- On drop, calculates new `displayOrder` values for all affected rows
- Sends a `PUT /api/components/reorder` request with the updated order array
- Shows a success toast on completion

**Status badge colours:**
- `operational` → green
- `degraded_performance` → yellow
- `partial_outage` → orange
- `major_outage` → red
- `under_maintenance` → blue

---

## 125. Issues/Incidents Page (`issues.js`)

**File:** `static/js/admin/issues/issues.js`

**Incident timeline:** Each incident row expands to show the update feed with timestamps and status transitions.

**Update feed:** Loaded via `GET /api/incidents/{id}/updates`, displayed chronologically.

**Resolve workflow:**
1. Click "Resolve" button on an open incident
2. Confirmation dialog appears
3. On confirm, calls `PATCH /api/incidents/{id}/resolve`
4. Row updates to show "Resolved" status and `resolvedTime`

---

## 126. Health Checks Page (`health-checks.js`)

**File:** `static/js/admin/health-checks/health-checks.js`

**Settings form fields:**
- Enabled toggle
- Check interval (seconds)
- Timeout (seconds)
- Expected HTTP status code
- Check URL (override)

**Manual trigger button:** Calls `POST /api/health-checks/trigger/app/{appId}` or per-component variant. Shows the result (UP/DOWN) in a status badge inline.

**Status display:** Last check time, last check result, and trend indicator (stable, improving, degrading).

---

## 127. Logs Page (`logs.js`)

**File:** `static/js/admin/logs/logs.js`

**Filters:**
- Service name (dropdown populated from `GET /api/logs/services`)
- Severity (INFO, WARN, ERROR, DEBUG)
- Date range (from / to)
- Message search (text input, queries `message` field)

**Column chooser:** User can show/hide columns. State is persisted to localStorage keyed by `{username}:logs:columns`.

**Real-time refresh:** Optional auto-refresh every 30 seconds (toggle in the UI). New entries are prepended to the top.

**Pagination:** Uses infinite scroll or page navigation (check the implementation).

---

## 128. Log Metrics Page (`log-metrics.js`)

**File:** `static/js/admin/log-metrics/log-metrics.js`

**Chart types:**
- Bar chart: log count by severity over time
- Line chart: error rate trend over time
- Service breakdown: stacked bar per service

**Time range selector:** Last 1h, 6h, 24h, 7d, 30d — sets the `from`/`to` query params.

**Data refresh:** Charts refresh when the time range is changed. Manual refresh button available.

**Data source:** `GET /api/log-metrics` with time range and optional service filter.

---

## 129. Alert Rules Page (`alert-rules.js`)

**File:** `static/js/admin/alert-rules/alert-rules.js`

**Rule builder fields:**
- Name
- Field to evaluate (`severity`, `message`, `serviceName`, etc.)
- Operator (`equals`, `contains`, `matches`, `greater_than`)
- Value / threshold
- Notification email

**Toggle UX:** Toggle button on each row calls `PATCH /api/alert-rules/{id}/toggle`. The row updates immediately without a full page reload.

---

## 130. Drop Rules Page (`drop-rules.js`)

**File:** `static/js/admin/drop-rules/drop-rules.js`

**Pattern input:** Free-text field accepting regex patterns or literal strings. The UI labels indicate that values are treated as regex.

**Rule table:** Columns: Name, Field, Pattern, Status, Actions. Ordered by creation date.

**Toggle:** Same pattern as alert rules — `PATCH /api/drop-rules/{id}/toggle`.

---

## 131. Process Mining Page (`process-mining.js`)

**File:** `static/js/admin/process-mining/process-mining.js`

**Visualisation type:** Timeline/swimlane view showing trace events grouped by `traceId`.

**Data shape:** List of traces, each with a sequence of log events. Rendered as horizontal bars on a time axis.

**Filter controls:**
- Service name
- Time range (from/to)
- Trace field selector (which log field to group by)

**Data source:** `GET /api/logs/process-mining` with filter params.

---

## 132. Public Status Page (`status.js`)

**File:** `static/js/public/status/status.js`

**Unauthenticated rendering:** No `Authorization` header is sent. Uses only `/api/public/status/{slug}/*` endpoints.

**Incident banner logic:**
- If there are active incidents with severity `major` or `critical`, a red banner is shown at the top
- If there are only `minor` incidents, a yellow banner is shown
- If everything is operational, a green "All systems operational" banner is shown

**Auto-refresh:** Page data refreshes every 60 seconds to keep status current.

---

## 133. LocalStorage Persistence

Per-user, per-table state is persisted in localStorage using this key naming convention:

```
{username}:{tableId}:{stateType}
```

Examples:
```
admin:platforms:columns       → visible column list
admin:platforms:filters       → active filter values
admin:logs:columns            → visible log columns
superadmin:tenants:sort       → sort field and direction
```

**Why per-user?** Multiple users may share a browser (e.g., on a shared workstation). Keying by username prevents one user's column preferences from overwriting another's.

**Reset:** Clearing localStorage removes all saved state. No server-side persistence is used for UI preferences.

---

## 134. Tabler.io Component Usage Guide

The application uses **Tabler.io** (v1.x) built on Bootstrap 5.

**Components used:**

| Component | Where used |
|-----------|-----------|
| `card` | Dashboard widgets, form containers |
| `table` | All data tables |
| `badge` | Status indicators |
| `btn` | All action buttons |
| `modal` | Create/edit forms, confirmations |
| `alert` | Flash messages and inline notifications |
| `toast` | Auto-dismiss notifications |
| `navbar` / `sidebar` | Navigation |
| `form-*` | All form inputs and labels |
| `dropdown` | Column chooser, filter menus |
| `pagination` | Table pagination controls |

**Icon library:** Tabler Icons (included in `tabler.min.js`). Usage: `<i class="ti ti-brand-github"></i>`

**Colour tokens:** Tabler provides semantic colours: `text-success`, `text-danger`, `text-warning`, `text-info`, `bg-success-lt`, `badge bg-danger-lt`, etc.

---

## 135. Local Asset Inventory

All frontend dependencies are served locally — no CDN.

**CSS files in `/static/css/`:**

| File | Version | Purpose |
|------|---------|---------|
| `tabler.min.css` | Tabler 1.x | Full Tabler.io + Bootstrap 5 CSS |
| `admin.css` | — | Custom application-specific styles |

**JavaScript files in `/static/js/vendor/`:**

| File | Version | Purpose |
|------|---------|---------|
| `bootstrap.bundle.min.js` | Bootstrap 5.x | Bootstrap JS (includes Popper.js) |
| `tabler.min.js` | Tabler 1.x | Tabler component JS (dropdowns, modals, toasts) |

**Fonts and icons:** Bundled within `tabler.min.css` — no separate font files required.

**To update a vendor library:**
1. Download the new version from the official source
2. Replace the file in `/static/js/vendor/` or `/static/css/`
3. Verify the application still works (visual regression check)
4. Document the new version in this inventory
