# Section 7: Service Layer

All services are in `src/main/java/org/automatize/status/services/`.
Services contain all business logic. Controllers must not bypass the service layer.

---

## 98. `StatusPlatformService`

**File:** `services/StatusPlatformService.java`

**Business rules:**
- Platform names must be unique within an organisation
- Platforms are scoped to an organisation; cross-org access is blocked
- Deleting a platform cascades to all associated `StatusApp` records (database-level cascade)
- Status updates propagate: if any child app has an active incident, the platform status reflects degradation

**Key methods:**
- `findAll(UUID orgId, Pageable)` — paginated list scoped to org
- `create(StatusPlatformRequest, UserPrincipal)` — validates uniqueness, creates record
- `update(UUID id, StatusPlatformRequest, UserPrincipal)` — validates ownership
- `delete(UUID id, UserPrincipal)` — deletes with cascade
- `updateStatus(UUID id, String status, UserPrincipal)` — patch status only

---

## 99. `StatusAppService`

**File:** `services/StatusAppService.java`

**Slug generation:**
- Slug is derived from the app name: lowercase, spaces → hyphens, special chars removed
- Uniqueness is enforced across all apps in the database (not just within the org) because slugs are used in public URLs
- If the derived slug already exists, a numeric suffix is appended (e.g., `my-app-2`)

**Status transitions:**
- `operational` → `degraded_performance` → `partial_outage` → `major_outage` → `under_maintenance`
- The `StatusAppController` exposes a `PATCH /{id}/status` endpoint for direct status overrides
- Health checks also update app status based on check results

**Key methods:**
- `findBySlug(String slug)` — public status page lookup
- `findAllByPlatform(UUID platformId, Pageable)` — platform-scoped listing
- `create/update/delete(...)` — standard CRUD

---

## 100. `StatusComponentService`

**File:** `services/StatusComponentService.java`

**Display order management:**
- Each component has a `displayOrder` integer
- New components are appended at the end (max existing order + 1)
- The `reorder(List<ComponentOrderRequest>)` method accepts a list of `{id, displayOrder}` pairs and updates all in a single transaction

**Key methods:**
- `findAllByApp(UUID appId)` — returns components sorted by `displayOrder ASC`
- `reorder(List<ComponentOrderRequest>, UserPrincipal)` — bulk display order update
- Standard CRUD

---

## 101. `StatusIncidentService`

**File:** `services/StatusIncidentService.java`

**Incident status lifecycle:**
`investigating` → `identified` → `monitoring` → `resolved`

**Business rules:**
- Creating an incident automatically links it to specified components
- The affected app's status is updated to reflect the incident severity
- Resolving an incident sets `resolvedTime` to the current timestamp
- Resolved incidents still appear in history but do not affect current status

**Key methods:**
- `create(StatusIncidentRequest, UserPrincipal)` — creates incident and links components, updates app status
- `addUpdate(UUID incidentId, StatusIncidentUpdateRequest, UserPrincipal)` — appends timeline entry
- `resolve(UUID id, UserPrincipal)` — marks resolved, updates app status
- `findActiveByApp(UUID appId)` — active (non-resolved) incidents for an app

---

## 102. `StatusMaintenanceService`

**File:** `services/StatusMaintenanceService.java`

**Window validation:**
- `endTime` must be strictly after `startTime` — validated in `create()` and `update()`
- Throws `IllegalArgumentException` if validation fails

**Status transitions:**
- `scheduled` → `in_progress` (via `start()` method or `PATCH /start` endpoint)
- `in_progress` → `completed` (via `complete()` method or `PATCH /complete` endpoint)

**Key methods:**
- `findUpcoming(UUID orgId)` — scheduled windows with `startTime > now`
- `findActive(UUID orgId)` — in-progress windows
- `start(UUID id, UserPrincipal)` — sets status to `in_progress` and updates affected component statuses
- `complete(UUID id, UserPrincipal)` — sets status to `completed`, restores component statuses

---

## 103. `HealthCheckService`

**File:** `services/HealthCheckService.java`

**Check types supported:**
- **HTTP/HTTPS:** Sends a GET request to the configured URL; checks response code against `expectedStatusCode` (default: 200)
- **TCP:** Attempts a socket connection to `host:port`

**Execution:**
- Each check runs in a thread from the `health-check.thread-pool-size` pool
- Timeout is applied from `HealthCheckSettings` or the global default
- Result: `UP`, `DOWN`, or `UNKNOWN`

**Result interpretation:**
- `UP` → successful response within timeout with expected status code
- `DOWN` → connection refused, timeout, or unexpected status code
- Status change from `UP` to `DOWN` triggers an update to the app/component status

---

## 104. `HealthCheckSettingsService`

**File:** `services/HealthCheckSettingsService.java`

**Settings hierarchy (most specific wins):**
1. Per-component `HealthCheckSettings` record
2. Per-app `HealthCheckSettings` record
3. Global `application.properties` defaults

`resolveEffectiveSettings(StatusComponent component)` returns the effective settings by walking the hierarchy.

---

## 105. `HealthCheckScheduler`

**File:** `services/HealthCheckScheduler.java`

**Scheduling:**
- `@Scheduled(fixedRateString = "${health-check.scheduler-interval-ms}")` (default: every 10 seconds)
- On each tick, queries for checks where `lastCheckedAt < now - intervalSeconds`
- Dispatches due checks to `HealthCheckService` via the async thread pool

**Stale check avoidance:**
- Tracks `lastCheckedAt` on each check to prevent overlapping executions
- If a check is still in-flight when the scheduler ticks again, it is skipped until the in-flight check completes

**Disabling:** Set `health-check.enabled=false` — the scheduler method checks this flag and returns early if disabled.

---

## 106. `UptimeHistoryService`

**File:** `services/UptimeHistoryService.java`

**Daily uptime calculation algorithm:**
1. For a given app and date, count all health check results recorded that day
2. Count how many had a `UP` status
3. `uptimePercentage = (successfulChecks / totalChecks) × 100`
4. Upsert the `StatusUptimeHistory` record for that app/date

**Backfill:** The `/api/uptime-history/backfill` endpoint triggers `calculateForDateRange(appId, startDate, endDate)` which calls the calculation for each day in the range.

**Daily trigger:** `LogRetentionScheduler` (or a dedicated scheduler) calls `calculateForAllApps(yesterday)` at midnight.

---

## 107. `LogIngestionService`

**File:** `services/LogIngestionService.java`

**Processing flow:**
1. Authenticate the `X-Api-Key` header against enabled `LogApiKey` records
2. Resolve the organisation from the API key
3. **Evaluate drop rules:** iterate enabled `DropRule` records for the org; if any rule matches the log, discard and return success without storing
4. Store the `Log` entity
5. Update `lastUsedAt` on the API key

**Batch limits:** Batch ingestion accepts up to a configured maximum number of entries per request (check `LogBatchRequest` for the `@Size` annotation limit).

**Drop rule evaluation:** Rules are evaluated in creation order. First match causes the log to be dropped.

---

## 108. `LogMetricService`

**File:** `services/LogMetricService.java`

**Aggregation:**
- Groups raw `Log` entries by `(serviceName, severity, timeBucket)` within an organisation
- Time buckets are configurable (e.g., 5-minute, 1-hour windows)
- Upserts `LogMetric` records for each bucket

The `LogMetricScheduler` calls this service periodically to keep metrics current without scanning all logs on every chart request.

---

## 109. `AlertRuleService`

**File:** `services/AlertRuleService.java`

**Condition types supported:**

| Operator | Applies to | Example |
|----------|-----------|---------|
| `equals` | String fields | `severity equals ERROR` |
| `contains` | String fields | `message contains "OutOfMemory"` |
| `greater_than` | Numeric | Count threshold |
| `less_than` | Numeric | |
| `matches` | Regex | `message matches ".*exception.*"` |

**Evaluation:** `AlertEvaluatorScheduler` calls `AlertRuleService.evaluateRules(UUID orgId)` on each scheduler tick.

---

## 110. `AlertEvaluatorScheduler`

**File:** `services/AlertEvaluatorScheduler.java`

**Cadence:** Runs on a fixed schedule (configured in the class or via a property).

**Deduplication:** Tracks recently fired alerts to avoid repeated notifications for the same rule matching the same data. Uses an in-memory cache with a cooldown period (typically 5–15 minutes) before re-firing the same rule.

---

## 111. `DropRuleService`

**File:** `services/DropRuleService.java`

**Pattern matching:**
- Patterns are compiled as Java `Pattern` (regex) for `matches` rules
- Simple `contains` patterns use `String.contains()`
- Rules are sorted by creation date; first match wins

**Performance:** For high-volume log ingestion, keep the number of enabled drop rules small. Compiled patterns are cached for the duration of the request. Consider adding an in-memory cache of compiled patterns if ingestion rates are high.

---

## 112. `IncidentNotificationService`

**File:** `services/IncidentNotificationService.java`

**Notification triggers:**
- New incident created → notify all confirmed subscribers of the affected app
- Incident updated → notify subscribers with the update content
- Incident resolved → notify subscribers with resolution summary

**Email template:** Uses a simple HTML email template. Falls back to plain text if the HTML template is unavailable.

**Error handling:** Email sending failures are caught and logged but do not fail the incident creation transaction. Uses `@Async` to prevent blocking.

---

## 113. `EmailService`

**File:** `services/EmailService.java`

**SMTP configuration:**
- Uses Spring's `JavaMailSender`
- Configured via `spring.mail.*` properties
- Async sending via `@Async` to prevent blocking the caller

**Development mode:** When `app.email.enabled=false`, the service logs the email content instead of sending it. Useful for development without an SMTP server.

**Error handling:**
- `MailException` is caught and logged at ERROR level
- The exception is not re-thrown — email failures are silent (fire-and-forget)

---

## 114. `ProcessMiningService`

**File:** `services/ProcessMiningService.java`

**What process mining represents:**
- Analyses structured logs to reconstruct execution traces
- Groups log entries by a trace identifier (e.g., `traceId`) to form a sequence of events
- Useful for visualising the flow of requests through a distributed system

**Input log fields used:**
- `traceId` — groups events into a single trace (case)
- `spanId` — orders events within a trace
- `timestamp` — chronological ordering
- `message` / `severity` — event content

**Output:** A list of traces, each with an ordered sequence of events. Statistics include average trace length, error rate per trace, and most common execution paths.

---

## 115. `AuthService`

**File:** `services/AuthService.java`

**Login flow:**
1. `AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)`
2. If successful, extract `UserPrincipal` from `Authentication`
3. Check if the user is a `SUPERADMIN` — if so, set `requiresContextSelection = true` in the token
4. Generate `accessToken` via `JwtUtils.generateJwtToken(authentication)`
5. Generate `refreshToken` via `JwtUtils.generateRefreshToken(username)`
6. Return `AuthResponse` with both tokens and user metadata

**Token refresh flow:**
1. Validate the refresh token via `JwtUtils.validateJwtToken(refreshToken)`
2. Extract username from the token
3. Load the current `User` from DB (validates the user still exists and is enabled)
4. Generate a new access token and refresh token
5. Return the new `AuthResponse`

---

## 116. `LogRetentionScheduler`

**File:** `services/LogRetentionScheduler.java`

**Schedule:** `@Scheduled(cron = "0 0 2 * * ?")` — runs daily at 02:00 server time

**What is deleted:** All `Log` records where `timestamp < (now - logs.retention.days × 86400000)`

**What is NOT deleted:** `LogMetric` records are retained independently — they serve as long-term summaries even after raw logs are purged.

**Verification:** Check the application log at 02:00 for messages like `Deleted N log entries older than 30 days`.
