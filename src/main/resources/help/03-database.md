# Section 3: Database & Migrations

---

## 21. Flyway Overview

Flyway manages all database schema changes. On every application startup, Flyway:

1. Connects to the database
2. Reads the `flyway_schema_history` table to determine the applied version
3. Runs any new migration scripts in version order
4. Records each applied migration with a checksum

**Migration script location:** `src/main/resources/db/migration/`

**Naming convention:** `V{version}__{description}.sql`
- Version: integer, must be higher than the previous migration
- Double underscore `__` separates version from description
- Description: words separated by underscores

Examples:
```
V1__init.sql
V2__logs_hub.sql
V11__alert_drop_rules.sql
V12__log_api_keys.sql
V13__logs_and_metrics.sql
```

**Key properties:**
- `spring.flyway.baseline-on-migrate=true` — allows Flyway to manage databases that existed before Flyway was introduced
- `spring.flyway.ignore-missing-migrations=true` — allows gaps (V1, V2, V11 skipping V3–V10)

---

## 22. V1__init.sql Walkthrough

Creates the foundational multi-tenant schema.

**Tables created:**

| Table | Description | Primary Key |
|-------|-------------|-------------|
| `tenants` | Top-level isolation boundary | `id UUID` (pgcrypto) |
| `organizations` | Belongs to a tenant | `id UUID` |
| `users` | App users bound to an org | `id UUID` |
| `status_platforms` | Monitored platform groups | `id UUID` |
| `status_apps` | Individual applications | `id UUID` |
| `status_components` | Components of an app | `id UUID` |
| `status_incidents` | Incidents affecting components | `id UUID` |
| `status_incident_updates` | Timeline updates on incidents | `id UUID` |
| `status_incident_components` | Junction: incident ↔ component | composite PK |
| `status_maintenances` | Scheduled maintenance windows | `id UUID` |
| `status_maintenance_components` | Junction: maintenance ↔ component | composite PK |
| `status_uptime_histories` | Daily uptime aggregations | `id UUID` |
| `notification_subscribers` | Email subscribers per app | `id UUID` |
| `platform_events` | External events posted per component | `id UUID` |
| `health_check_settings` | Per-app/component health check config | `id UUID` |

**Standard columns on every table:**
- `created_by VARCHAR`
- `created_date TIMESTAMP`
- `last_modified_by VARCHAR`
- `last_modified_date TIMESTAMP`

**Key constraints:**
- Foreign keys with `ON DELETE CASCADE` for component deletions
- `UNIQUE` constraint on `users.username` and `users.email`
- `UNIQUE` constraint on `status_apps.slug` for public URL uniqueness

---

## 23. V2__logs_hub.sql Walkthrough

Adds the Logs Hub feature — structured log ingestion from external services.

**Tables created:**

| Table | Description |
|-------|-------------|
| `logs` | Individual log entries with service name, severity, message, and metadata |
| `log_api_keys` | API keys for authenticating log ingestion requests |

**`logs` table key columns:**
- `id UUID` — primary key
- `service_name VARCHAR` — source service identifier
- `severity VARCHAR` — log level (INFO, WARN, ERROR, DEBUG, etc.)
- `message TEXT` — log message body
- `timestamp BIGINT` — epoch milliseconds (for fast range queries)
- `organization_id UUID` — tenant isolation FK
- `metadata JSONB` — arbitrary structured metadata

**Index strategy:** Composite index on `(organization_id, timestamp DESC)` for paginated log queries. Index on `service_name` for service filter queries.

---

## 24. V11__alert_drop_rules.sql Walkthrough

Adds alerting and log filtering rules.

**Tables created:**

| Table | Description |
|-------|-------------|
| `alert_rules` | Conditions that trigger notifications when log patterns match |
| `drop_rules` | Patterns that silently discard matching log entries before storage |

**`alert_rules` key columns:**
- Condition field, operator, threshold value
- Notification target (email address or webhook)
- `enabled BOOLEAN` — can be toggled without deletion
- `organization_id UUID` — scoped per organisation

**`drop_rules` key columns:**
- `field VARCHAR` — which log field to match against
- `pattern VARCHAR` — regex or literal pattern
- `enabled BOOLEAN`
- `organization_id UUID`

---

## 25. V12__log_api_keys.sql Walkthrough

Refines the `log_api_keys` table introduced in V2.

Changes from V2:
- Adds `key_hash VARCHAR` column — only the hash is stored; the plain key is shown once at creation
- Adds `description VARCHAR` — human-readable label for the key
- Adds `last_used_at TIMESTAMP` — tracks key usage recency
- Adds `enabled BOOLEAN` — allows disabling without deletion

The `key_hash` approach prevents key leakage if the database is compromised.

---

## 26. V13__logs_and_metrics.sql Walkthrough

Adds the log metrics aggregation table.

**Table created:**

| Table | Description |
|-------|-------------|
| `log_metrics` | Pre-aggregated log counts bucketed by time, service, and severity |

**`log_metrics` key columns:**
- `bucket_start TIMESTAMP` — start of the time window
- `bucket_end TIMESTAMP` — end of the time window
- `service_name VARCHAR` — which service these metrics cover
- `severity VARCHAR` — log level bucket
- `count BIGINT` — number of log entries in this bucket
- `organization_id UUID` — tenant isolation

**Purpose:** Raw logs are numerous and expensive to query for charting. The `LogMetricScheduler` periodically aggregates raw logs into time buckets in this table, enabling fast metric queries without scanning the full `logs` table.

---

## 27. Writing a New Migration

**Step-by-step:**

1. Find the highest existing version number (currently V13)
2. Create `src/main/resources/db/migration/V14__{description}.sql`
3. Write idempotent SQL (use `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`)
4. Test locally: drop and recreate the `uptime` database, then start the app
5. Verify the migration appears in `flyway_schema_history`

**DO:**
- Use `IF NOT EXISTS` / `IF EXISTS` guards
- Include `NOT NULL` with `DEFAULT` when adding columns to populated tables
- Add indices for foreign key columns and frequently filtered columns

**DON'T:**
- Rename or delete columns in a single migration when data migration is needed — use two migrations
- Modify an already-applied migration (Flyway checksums will fail)
- Drop tables without confirming no application code references them

---

## 28. Rollback Strategy

Flyway Community Edition has **no built-in rollback**. The documented procedure for reverting a bad migration:

1. **Stop the application**
2. **Manually revert the schema** via psql (reverse the DDL changes)
3. **Delete the migration record** from `flyway_schema_history` for the bad version:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '14';
   ```
4. **Fix or remove** the migration SQL file
5. **Restart the application** — Flyway will re-run the corrected migration

For production, prefer writing a `V{n+1}__revert_xxx.sql` forward migration that undoes the change, rather than manipulating `flyway_schema_history` directly.

---

## 29. Audit Columns Standard

Every entity table includes these four audit columns:

| Column | Type | Description |
|--------|------|-------------|
| `created_by` | VARCHAR | Username of the user who created the record |
| `created_date` | TIMESTAMP | When the record was created |
| `last_modified_by` | VARCHAR | Username of the last user who modified the record |
| `last_modified_date` | TIMESTAMP | When the record was last modified |

These are populated automatically by Spring Data JPA auditing (`@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate`). The `@EnableJpaAuditing` annotation must be present for this to work.

---

## 30. Technical Timestamp Convention

Many entities have both a JPA `TIMESTAMP` audit column and an additional `BIGINT` epoch milliseconds column (e.g., `timestamp` on `Log`).

**Why both?**
- `TIMESTAMP` audit columns are managed by Spring JPA and are human-readable for database inspection
- `BIGINT` epoch milliseconds fields are used for high-performance range queries and are sortable without casting
- External systems (e.g., log shippers) often submit epoch ms timestamps that are stored directly without conversion

When writing queries against the `logs` table, always filter on the `timestamp` (BIGINT) column rather than `created_date` for performance.

---

## 31. UUID Generation

All primary keys are UUIDs generated by PostgreSQL's `pgcrypto` extension:

```sql
id UUID DEFAULT gen_random_uuid() PRIMARY KEY
```

**Why UUIDs?**
- No sequential enumeration attacks (unlike auto-increment integers)
- Safe to expose in public URLs and API responses
- Application-side UUID generation is also supported where needed

**When auto-increment IDs are acceptable:** For internal junction tables where the ID is never exposed externally (e.g., `status_incident_components`).

---

## 32. Soft Delete Policy

The system does **not** implement a global soft delete pattern. Records are hard-deleted when removed. Exceptions:

- `users.enabled` — users are disabled (soft delete) rather than deleted to preserve audit history
- `log_api_keys.enabled` — keys are disabled rather than deleted to preserve usage records
- `alert_rules.enabled` / `drop_rules.enabled` — rules are toggled, not deleted

If you need to add soft delete to an entity, add a `deleted_at TIMESTAMP` column and filter `WHERE deleted_at IS NULL` in all repository queries.

---

## 33. Index Strategy

| Table | Index | Purpose |
|-------|-------|---------|
| `logs` | `(organization_id, timestamp DESC)` | Paginated log queries per org |
| `logs` | `service_name` | Filter by service |
| `log_metrics` | `(organization_id, bucket_start)` | Metric time-range queries |
| `status_apps` | `slug` UNIQUE | Public status page URL lookup |
| `users` | `username` UNIQUE | Login lookup |
| `users` | `email` UNIQUE | Registration duplicate check |
| `platform_events` | `(organization_id, created_date DESC)` | Recent events per org |

When adding new query patterns that filter on non-indexed columns at scale, add a migration with the appropriate index.

---

## 34. PostgreSQL-Specific Features Used

| Feature | Where used | Purpose |
|---------|-----------|---------|
| `pgcrypto` extension | All primary keys | `gen_random_uuid()` for UUID PKs |
| `JSONB` type | `logs.metadata` | Flexible structured metadata on log entries |
| `UUID` type | All primary keys and FKs | Type-safe UUIDs |
| `TIMESTAMP WITH TIME ZONE` | Audit columns | Timezone-aware datetime storage |
| `BIGINT` | `logs.timestamp` | Epoch milliseconds for fast range queries |

---

## 35. Connection Pool Configuration

The application uses HikariCP (Spring Boot default). Default settings work for development. For production tuning:

| Property | Default | Production recommendation |
|----------|---------|--------------------------|
| `spring.datasource.hikari.maximum-pool-size` | 10 | Set to 2–4× CPU cores |
| `spring.datasource.hikari.minimum-idle` | 10 | Set to 2–5 |
| `spring.datasource.hikari.connection-timeout` | 30000ms | 30s is fine |
| `spring.datasource.hikari.idle-timeout` | 600000ms | 10 min is fine |
| `spring.datasource.hikari.max-lifetime` | 1800000ms | 30 min; set lower than PostgreSQL `idle_in_transaction_session_timeout` |

Add these to `application-prod.properties` to override defaults in production.

Signs of pool exhaustion: `HikariPool - Connection is not available, request timed out` in logs. Increase `maximum-pool-size` or investigate slow queries holding connections.
