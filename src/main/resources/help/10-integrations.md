# Section 10: Integrations & External Interfaces

---

## 142. Log Ingestion Integration Guide

External services can POST structured logs to the Logs Hub.

### Authentication

Include your Log API key in every request:
```
X-Api-Key: <your-log-api-key>
```

Generate API keys from the Admin UI → Log API Keys page.

### Single Log Entry

**Endpoint:** `POST /api/logs`

**Headers:**
```
Content-Type: application/json
X-Api-Key: <your-log-api-key>
```

**Request body:**
```json
{
  "serviceName": "payment-service",
  "severity": "ERROR",
  "message": "Payment processing failed for order ORD-001",
  "timestamp": 1711598400000,
  "traceId": "trace-abc123",
  "spanId": "span-def456",
  "metadata": {
    "orderId": "ORD-001",
    "userId": "user-789",
    "errorCode": "PAYMENT_DECLINED"
  }
}
```

**Field reference:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serviceName` | String | Yes | Identifier for your service |
| `severity` | String | Yes | `INFO`, `WARN`, `ERROR`, `DEBUG`, `TRACE` |
| `message` | String | Yes | Log message content |
| `timestamp` | Long | No | Epoch milliseconds; defaults to server receive time |
| `traceId` | String | No | Distributed trace ID for correlation |
| `spanId` | String | No | Span ID within the trace |
| `metadata` | Object | No | Any additional key-value pairs as JSON |

**Response:** `201 Created` on success. `401 Unauthorized` if the API key is invalid or disabled.

### Batch Log Ingestion

**Endpoint:** `POST /api/logs/batch`

**Request body:**
```json
{
  "logs": [
    {
      "serviceName": "api-gateway",
      "severity": "INFO",
      "message": "Request processed",
      "timestamp": 1711598400000
    },
    {
      "serviceName": "api-gateway",
      "severity": "ERROR",
      "message": "Upstream timeout",
      "timestamp": 1711598401000
    }
  ]
}
```

**Limits:** Maximum batch size is enforced by the `@Size` annotation on `LogBatchRequest.logs`. Check the current limit before sending large batches.

### Integration Examples

**curl:**
```bash
curl -X POST http://localhost:8383/api/logs \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-api-key" \
  -d '{"serviceName":"myapp","severity":"ERROR","message":"Something failed"}'
```

**Java (Spring RestTemplate):**
```java
HttpHeaders headers = new HttpHeaders();
headers.set("X-Api-Key", apiKey);
headers.setContentType(MediaType.APPLICATION_JSON);
HttpEntity<LogRequest> request = new HttpEntity<>(logRequest, headers);
restTemplate.postForEntity("http://status-app/api/logs", request, Void.class);
```

---

## 143. Platform Event Integration Guide

Platform events allow external systems to post deployment events, configuration changes, or custom annotations that appear in the admin events timeline.

### Generating an Event API Key

1. Admin UI → Events → select a platform
2. Click "Generate Key" — a key is generated and shown once
3. Copy the key — it cannot be viewed again

### Posting an Event

**Endpoint:** `POST /api/events/log`

**Headers:**
```
Content-Type: application/json
X-Api-Key: <your-event-api-key>
```

**Request body:**
```json
{
  "eventType": "deploy",
  "message": "Deployed version 2.3.1 to production",
  "appId": "<uuid-of-the-app>",
  "componentId": "<uuid-of-component-optional>"
}
```

**Common event types:**

| Type | When to use |
|------|------------|
| `deploy` | New version deployed |
| `config_change` | Configuration updated |
| `scale` | Scaling event (up or down) |
| `restart` | Service restart |
| `maintenance_start` | Maintenance window started |
| `maintenance_end` | Maintenance window completed |
| `alert` | Custom alert or notification |

**CI/CD integration example (GitHub Actions):**
```yaml
- name: Post deploy event
  run: |
    curl -X POST ${{ secrets.STATUS_APP_URL }}/api/events/log \
      -H "Content-Type: application/json" \
      -H "X-Api-Key: ${{ secrets.STATUS_EVENT_KEY }}" \
      -d "{\"eventType\":\"deploy\",\"message\":\"Deployed ${{ github.sha }}\",\"appId\":\"${{ vars.STATUS_APP_ID }}\"}"
```

---

## 144. Email Integration

### SMTP Requirements

Configure an SMTP server in `application.properties`:

```properties
app.email.enabled=true
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=noreply@yourdomain.com
spring.mail.password=your-smtp-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

**Common SMTP providers:**

| Provider | Host | Port | Notes |
|----------|------|------|-------|
| Gmail | `smtp.gmail.com` | 587 | Requires App Password if 2FA enabled |
| Outlook/Office 365 | `smtp-mail.outlook.com` | 587 | |
| Amazon SES | `email-smtp.{region}.amazonaws.com` | 587 | Requires verified sender domain |
| Mailgun | `smtp.mailgun.org` | 587 | |
| SendGrid | `smtp.sendgrid.net` | 587 | Use API key as password |

### SPF / DKIM Recommendations

- Add an SPF record to your DNS that includes your SMTP provider's IP ranges
- Enable DKIM signing in your email provider dashboard
- Consider adding a DMARC policy to prevent spoofing

### Template Customisation

Email templates are generated in `EmailService` and `IncidentNotificationService`. To customise the format, modify the HTML template strings in those service classes. A future improvement would be to move templates to `resources/templates/email/`.

---

## 145. Webhook / Notification Subscriber Integration

Status page visitors can subscribe to incident notifications for a specific application.

### Subscription Flow

1. Visitor clicks "Subscribe" on the public status page
2. Enters their email address
3. Receives a confirmation email with a one-time confirmation link
4. Clicking the link sets `confirmed = true` on their `NotificationSubscriber` record
5. They now receive emails for new incidents, updates, and resolutions

### Notifications Sent

| Event | When |
|-------|------|
| New incident created | Immediately when an incident is opened |
| Incident updated | When a new `StatusIncidentUpdate` is added |
| Incident resolved | When the incident status changes to `resolved` |

### Admin Management

View and manage subscribers at Admin → Subscribers. Admins can delete subscriber records to manually unsubscribe users.

---

## 146. Public Status Page Embedding

The public status page can be embedded in other sites or accessed programmatically.

### iFrame Embedding

```html
<iframe
  src="https://your-status-app.com/my-app-slug"
  width="100%"
  height="600"
  frameborder="0"
  title="Service Status">
</iframe>
```

**CORS considerations:** The public status API endpoints (`/api/public/status/**`) allow cross-origin requests. The static page itself can be embedded in an iframe from any domain.

### Programmatic Access

All public endpoints are unauthenticated and CORS-enabled. External dashboards or status aggregators can poll them directly:

```javascript
// Fetch status from an external application
const response = await fetch('https://your-app.com/api/public/status/my-app-slug');
const status = await response.json();
console.log(status.overallStatus); // "operational" | "degraded" | etc.
```

See [Section 6, item 94](06-api-reference.md#94-public-status-endpoints-apipublicstatus) for the full public API reference.

---

## 147. OpenAPI Spec Export

The application generates an OpenAPI 3.0 specification automatically.

### Accessing the Spec

```
GET http://localhost:8383/v3/api-docs
GET http://localhost:8383/v3/api-docs.yaml
```

### Generating Client SDKs

Using the [OpenAPI Generator](https://openapi-generator.tech/):

```bash
# Install
npm install @openapitools/openapi-generator-cli -g

# Generate a TypeScript-Fetch client
openapi-generator-cli generate \
  -i http://localhost:8383/v3/api-docs \
  -g typescript-fetch \
  -o ./generated/status-client

# Generate a Java client
openapi-generator-cli generate \
  -i http://localhost:8383/v3/api-docs \
  -g java \
  -o ./generated/status-java-client
```

### Recommended Generator Settings

For TypeScript clients:
- `--additional-properties=supportsES6=true,typescriptThreePlus=true`

For Java clients:
- `-p library=resttemplate` or `-p library=feign`
