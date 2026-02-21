# Logs Hub — Simplified Implementation

# Logs Hub — Simplified Implementation

## Core Concept
Ingest logs via REST → store in DB → query/filter in UI. No third-party log platform required.

---

## 1. Log Ingestion (REST API)

- [x] POST `/api/logs` endpoint — accepts JSON log payloads from any service
  - Fields: `timestamp`, `level`, `service`, `message`, `metadata` (TEXT/JSON)
- [x] Batch ingestion support (`POST /api/logs/batch` with `{"logs":[...]}`)
- [x] API key auth on the ingestion endpoint (`X-Log-Api-Key` header, validated via `log_api_keys` table)

---

## 2. Storage (Database)

- [x] `logs` table with indexed columns: `log_timestamp`, `level`, `service`
- [x] TEXT column for `metadata` to allow flexible per-service JSON fields
- [ ] Partition table by time (e.g., monthly) to keep queries fast as volume grows *(deferred — use indexes for now)*
- [x] TTL / retention policy — auto-delete logs older than N days via `LogRetentionScheduler` (runs daily at 02:00, configured via `logs.retention.days`)

---

## 3. Search & Explore (UI)

- [x] Filter bar: filter by `service`, `level`, `time range`, free-text search on `message`
- [x] Results table with live-updating tail mode (poll every 3s via `toggleTail()`)
- [x] Click into a log line to see full `metadata` JSON (pretty-printed in modal)
- [x] URL-serialized filter state so queries are shareable (`?level=ERROR&service=api&q=...`)

---

## 4. Drop Filters / Cost Control

- [x] Config table: `drop_rules` — define rules to reject logs before they're stored (e.g., `level=INFO AND service=payments`)
- [x] Evaluate rules in the ingestion endpoint before writing to DB (`LogIngestionService.isDropped()`)
- [x] UI to add/disable/delete drop rules without touching code (`/admin/drop-rules`)

---

## 5. LogMetrics

- [x] Scheduled job: aggregate log counts by `service` + `level` into a `log_metrics` table per minute (`LogMetricScheduler` runs every minute via cron)
- [x] Expose via GET `/api/log-metrics` for dashboards
- [x] Dashboard at `/admin/log-metrics` with summary cards (total, errors, warnings, service count) and metrics table

---

## 6. Alerting

- [x] `alert_rules` table: define threshold conditions (e.g., `error count > 50 in 5 min for service=api`)
- [x] Scheduled job evaluates rules against `log_metrics` (`AlertEvaluatorScheduler` runs every minute)
- [x] Notification targets: Email (SMTP via `EmailService`), Slack webhook, generic outbound HTTP webhook
- [x] Cooldown/debounce per rule to prevent alert spam (`cooldown_minutes` column + `lastFiredAt` check)

---

## 7. Dashboards

- [x] Dashboard at `/admin/log-metrics`: log volume totals, error/warning counts, count by service+level
- [x] Uses `log_metrics` table — no expensive full log scans for charts
- [x] Time window selector (1h / 6h / 24h / 7d), auto-refreshes every 60s

---

## 8. Correlation (Optional / Later)

- [x] `trace_id` and `request_id` as first-class indexed columns on `logs`
- [ ] If you have distributed tracing data, link log lines to trace records by `trace_id` *(deferred)*
- [ ] Add `deployment` events table — mark deploys and overlay on log/metric charts *(deferred)*

---

## Implementation Summary

### New files created:

**Database:**
- `src/main/resources/db/migration/V2__logs_hub.sql`

**Models:**
- `models/Log.java`
- `models/LogApiKey.java`
- `models/DropRule.java`
- `models/LogMetric.java`
- `models/AlertRule.java`

**Repositories:**
- `repositories/LogRepository.java`
- `repositories/LogApiKeyRepository.java`
- `repositories/DropRuleRepository.java`
- `repositories/LogMetricRepository.java`
- `repositories/AlertRuleRepository.java`

**Services:**
- `services/LogIngestionService.java`
- `services/LogApiKeyService.java`
- `services/DropRuleService.java`
- `services/LogMetricService.java`
- `services/AlertRuleService.java`
- `services/LogMetricScheduler.java` (cron every minute)
- `services/LogRetentionScheduler.java` (daily at 02:00)
- `services/AlertEvaluatorScheduler.java` (cron every minute)

**API Requests:**
- `api/request/LogRequest.java`
- `api/request/LogBatchRequest.java`
- `api/request/DropRuleRequest.java`
- `api/request/AlertRuleRequest.java`
- `api/request/LogApiKeyRequest.java`

**API Responses:**
- `api/response/LogResponse.java`
- `api/response/DropRuleResponse.java`
- `api/response/LogMetricResponse.java`
- `api/response/AlertRuleResponse.java`
- `api/response/LogApiKeyResponse.java`

**REST Controllers:**
- `controllers/api/LogController.java` — `POST /api/logs`, `POST /api/logs/batch`, `GET /api/logs`, `GET /api/logs/{id}`, `GET /api/logs/services`
- `controllers/api/DropRuleController.java` — `GET/POST/PUT/DELETE /api/drop-rules`
- `controllers/api/LogMetricController.java` — `GET /api/log-metrics`
- `controllers/api/AlertRuleController.java` — `GET/POST/PUT/DELETE /api/alert-rules`
- `controllers/api/LogApiKeyController.java` — `GET/POST/DELETE /api/log-api-keys`

**Templates:**
- `templates/admin/logs.html`
- `templates/admin/drop-rules.html`
- `templates/admin/log-metrics.html`
- `templates/admin/alert-rules.html`
- `templates/admin/log-api-keys.html`

**JavaScript:**
- `static/js/admin/logs/logs.js`
- `static/js/admin/drop-rules/drop-rules.js`
- `static/js/admin/log-metrics/log-metrics.js`
- `static/js/admin/alert-rules/alert-rules.js`
- `static/js/admin/log-api-keys/log-api-keys.js`

**Config changes:**
- `application.properties` — added `logs.retention.days=30`
- `config/SecurityConfig.java` — permitted `/api/logs` and `/api/logs/batch` without JWT
- `controllers/AdminController.java` — added routes: `/admin/logs`, `/admin/drop-rules`, `/admin/log-metrics`, `/admin/alert-rules`, `/admin/log-api-keys`
