controllers# Section 13: Troubleshooting & Runbooks

---

## 164. App Won't Start

### Symptom: Application fails to start with an exception

**Check 1: Database connection failure**

Symptom in logs:
```
HikariPool-1 - Exception during pool initialization.
org.postgresql.util.PSQLException: Connection refused
```

Steps:
1. Verify PostgreSQL is running: `pg_isready -h localhost -p 5432`
2. Verify the `uptime` database exists: `psql -U postgres -l | grep uptime`
3. Verify credentials in `application.properties` or environment variables
4. If using Docker, ensure the PostgreSQL container is healthy before starting the app

---

**Check 2: Flyway checksum mismatch**

Symptom in logs:
```
FlywayException: Validate failed: Migration checksum mismatch for migration version 1
-> Applied to database : 1234567890
-> Resolved locally    : 9876543210
```

Cause: A previously applied migration file was modified.

Fix:
```sql
-- Do NOT do this lightly — only if you are certain about what changed
-- Update the checksum to match the current file
UPDATE flyway_schema_history
SET checksum = <new-checksum>
WHERE version = '1';
```

Better fix: Revert the migration file to its original content. Never modify applied migrations.

---

**Check 3: Missing environment variables / properties**

Symptom:
```
java.lang.IllegalArgumentException: Could not resolve placeholder 'jwt.secret' in value "${jwt.secret}"
```

Fix: Ensure `application.properties` contains `jwt.secret` or set the `JWT_SECRET` environment variable.

---

**Check 4: Hibernate schema validation failure**

Symptom:
```
SchemaManagementException: Schema-validation: missing table [xxx]
```

Cause: A new entity was added in code but no migration was written to create the table.

Fix: Write a new Flyway migration to create the missing table, then restart.

---

## 165. JWT Token Rejected

**Symptom:** API returns `401 Unauthorized` with a token that should be valid.

**Check 1: Token expired**

Tokens expire after 24 hours (access) or 7 days (refresh). Check the `exp` claim:
```bash
# Decode a JWT (splits on . and base64-decodes the payload)
echo "eyJ..." | cut -d. -f2 | base64 -d 2>/dev/null | python3 -m json.tool
```

Look at the `exp` field (epoch seconds). Compare to `date +%s`.

Fix: Use the refresh token to obtain a new access token.

---

**Check 2: Wrong JWT secret in production**

If `jwt.secret` was changed after tokens were issued, all existing tokens become invalid (signature mismatch).

Symptom in logs:
```
Invalid JWT token: JWT signature does not match locally computed signature
```

Fix: Users must re-authenticate. This is expected behaviour when rotating the secret.

---

**Check 3: Token format errors**

Symptom: `401` even with a freshly issued token.

Check:
- Token is correctly formatted: `Bearer <token>` (note the space after `Bearer`)
- No extra whitespace or newlines in the token value
- Token is the access token, not the refresh token (refresh tokens cannot be used for API access)

---

**Check 4: Clock skew**

If the server clock is significantly ahead of the client clock, tokens may be rejected as "issued in the future".

Fix: Synchronise server time with NTP: `timedatectl set-ntp true`

---

## 166. Health Checks Not Firing

**Symptom:** Health checks are configured but no results are appearing.

**Check 1: Master toggle**

```properties
health-check.enabled=true
```

If this is `false`, the scheduler method returns early without dispatching any checks.

---

**Check 2: Scheduler not running**

Look in startup logs for:
```
Health check scheduler started. Interval: 10000ms
```

If absent, the scheduler may not be running. Verify `@EnableScheduling` is present on the main application class or a configuration class.

---

**Check 3: Thread pool exhaustion**

Symptom: Health checks queued but not executing; thread pool size warnings in logs.

Fix: Increase `health-check.thread-pool-size` in `application.properties`.

---

**Check 4: Check interval too long**

Each health check has a `intervalSeconds` setting. If set to 3600 (1 hour), checks will only run once an hour. Verify the effective settings via the Health Checks admin page.

---

**Check 5: Health check URL unreachable**

The application must be able to reach the health check URLs from the server it's running on. Test manually:
```bash
curl -v http://your-target-url/health
```

---

## 167. Logs Not Ingesting

**Symptom:** External service posts logs but they don't appear in the admin logs page.

**Check 1: API key invalid or disabled**

Verify the key:
- Is the `X-Api-Key` header being sent with the correct key value?
- Is the key enabled in Admin → Log API Keys?
- Was the key recently regenerated? The old key value is now invalid.

---

**Check 2: Drop rules filtering everything**

Check Admin → Drop Rules. If a broad drop rule (e.g., matching all log entries) is enabled, logs will be silently dropped.

Test by temporarily disabling all drop rules and retrying.

---

**Check 3: API key belongs to wrong organisation**

Log API keys are scoped per organisation. If the key was created in Organisation A but the receiving endpoint routes to Organisation B, authentication will fail.

---

**Check 4: Batch size exceeded**

If posting a large batch, check the `@Size` constraint on `LogBatchRequest.logs`. Reduce the batch size and retry.

---

**Check 5: Content-Type header missing**

The ingestion endpoint requires `Content-Type: application/json`. If missing, the request body will not be parsed.

---

## 168. Email Notifications Not Sending

**Symptom:** Incidents are created but subscribers receive no email.

**Check 1: Email feature is disabled**

```properties
app.email.enabled=false
```

When false, emails are logged at INFO level instead of being sent. Set to `true` and restart.

---

**Check 2: SMTP configuration**

Look for SMTP errors in the logs:
```
MailSendException: Failed messages: com.sun.mail.smtp.SMTPSendFailedException
```

Verify:
- `spring.mail.host` is correct and reachable
- `spring.mail.username` / `spring.mail.password` are correct
- Port 587 is not blocked by a firewall
- STARTTLS settings match what the SMTP server requires

Test SMTP connectivity:
```bash
telnet smtp.example.com 587
```

---

**Check 3: Subscriber not confirmed**

Subscribers must confirm their email before receiving notifications (`confirmed = true`). Unconfirmed subscribers are excluded from notification dispatch.

Check in Admin → Subscribers — look for `Confirmed` status column.

---

**Check 4: Async task failure**

Email sending is `@Async` — failures are caught and logged but do not propagate. Check the logs at ERROR level around the time the incident was created.

---

## 169. Flyway Migration Failure

**Symptom:** Application fails to start with:
```
FlywayException: Found failed migration to version 14 (...)
```

**Cause:** A migration ran partially and failed partway through.

**Recovery steps:**

1. Check what failed:
   ```sql
   SELECT * FROM flyway_schema_history WHERE success = false;
   ```

2. Manually revert any partial changes from the failed migration in the database.

3. Delete the failed migration record:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '14' AND success = false;
   ```

4. Fix the migration SQL file.

5. Restart the application — Flyway will re-run the fixed migration.

**Prevention:** Always use transactions in migration scripts:
```sql
BEGIN;
ALTER TABLE status_apps ADD COLUMN new_field VARCHAR;
-- ... other changes ...
COMMIT;
```

If the transaction fails, the entire migration is rolled back automatically.

---

## 170. Performance Runbook

### Slow queries to investigate first

```sql
-- Long-running queries
SELECT pid, query_start, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active' AND now() - query_start > interval '5 seconds'
ORDER BY duration DESC;

-- Most frequently called slow queries
SELECT query, calls, mean_exec_time, total_exec_time
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

### Index usage verification

```sql
-- Tables with sequential scans (missing indexes)
SELECT relname, seq_scan, idx_scan
FROM pg_stat_user_tables
WHERE seq_scan > idx_scan
ORDER BY seq_scan DESC;
```

### Connection pool exhaustion signs

Logs: `HikariPool - Connection is not available, request timed out after 30000ms`

Check current pool usage:
```sql
SELECT count(*), state
FROM pg_stat_activity
WHERE datname = 'uptime'
GROUP BY state;
```

Fix: Increase `spring.datasource.hikari.maximum-pool-size` or investigate slow queries holding connections.

---

## 171. Alert Rules Not Triggering

**Symptom:** Alert rules are configured but alerts are never fired.

**Check 1: Alert evaluator is running**

Look for `AlertEvaluatorScheduler` activity in logs. If absent, the scheduler may not be configured.

---

**Check 2: Rule is disabled**

Check Admin → Alert Rules — verify the rule `enabled` toggle is on.

---

**Check 3: Condition threshold misconfigured**

Example: Alert rule configured for `count > 100` but only 50 error logs exist in the evaluation window.

Review the rule condition:
- Is the field name correct? (e.g., `severity` not `Severity`)
- Is the operator correct? (`equals`, `contains`, `greater_than`)
- Is the threshold value reasonable for your log volume?

---

**Check 4: No matching logs in the evaluation window**

Alert rules evaluate logs from the last N minutes (check the `AlertEvaluatorScheduler` configuration). If no logs matching the condition were ingested in that window, no alert fires.

---

**Check 5: Cooldown period active**

If an alert fired recently, the cooldown period may be preventing re-firing. This is by design (deduplication). Wait for the cooldown to expire.
