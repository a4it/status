# Logs Hub — Simplified Implementation

# Logs Hub — Simplified Implementation

## Core Concept
Ingest logs via REST → store in DB → query/filter in UI. No third-party log platform required.

---

## 2. Storage (Database)

- [x] `logs` table with indexed columns: `log_timestamp`, `level`, `service`
- [x] TEXT column for `metadata` to allow flexible per-service JSON fields
- [ ] Partition table by time (e.g., monthly) to keep queries fast as volume grows *(deferred — use indexes for now)*
- [x] TTL / retention policy — auto-delete logs older than N days via `LogRetentionScheduler` (runs daily at 02:00, configured via `logs.retention.days`)

---

## 8. Correlation (Optional / Later)

- [x] `trace_id` and `request_id` as first-class indexed columns on `logs`
- [ ] If you have distributed tracing data, link log lines to trace records by `trace_id` *(deferred)*
- [ ] Add `deployment` events table — mark deploys and overlay on log/metric charts *(deferred)*

---
