# Section 6: REST API Reference

---

## 74. API Conventions

**Base URL:** `http://localhost:8383` (development) / your production domain

**Content-Type:** `application/json` for all request and response bodies

**Authentication:**
```
Authorization: Bearer <accessToken>
```
Omit for public endpoints. Log ingestion uses `X-Api-Key` instead.

**Pagination parameters:**
```
GET /api/endpoint?page=0&size=20&sort=createdDate,desc
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | `0` | Zero-based page number |
| `size` | Integer | `20` | Items per page |
| `sort` | String | varies | Field name, optionally `,asc` or `,desc` |

**Pagination response envelope:**
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

## 75. Authentication Endpoints (`/api/auth`)

### POST `/api/auth/login`

**Request:**
```json
{
  "username": "admin",
  "password": "admin"
}
```

**Response:**
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<jwt>",
  "tokenType": "Bearer",
  "userId": "<uuid>",
  "username": "admin",
  "email": "admin@status.local",
  "role": "ADMIN",
  "organizationId": "<uuid>",
  "requiresContextSelection": false
}
```

### POST `/api/auth/refresh`

**Request:**
```json
{
  "refreshToken": "<refreshToken>"
}
```

**Response:** Same as login response with new tokens.

### POST `/api/auth/register` _(requires `app.registration.enabled=true`)_

**Request:**
```json
{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123"
}
```

### GET `/api/auth/me` _(authenticated)_

Returns the current user's profile.

### POST `/api/auth/logout`

Client-side: discard tokens. No server-side action (stateless).

---

## 76. Platform Endpoints (`/api/status-platforms`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/status-platforms` | List all platforms (paginated) |
| `GET` | `/api/status-platforms/{id}` | Get platform by ID |
| `POST` | `/api/status-platforms` | Create a platform |
| `PUT` | `/api/status-platforms/{id}` | Update a platform |
| `DELETE` | `/api/status-platforms/{id}` | Delete a platform |
| `PATCH` | `/api/status-platforms/{id}/status` | Update platform status |

**Query params for list:**
- `organizationId` — filter by org

---

## 77. Application Endpoints (`/api/status-apps`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/status-apps` | List all apps (paginated) |
| `GET` | `/api/status-apps/{id}` | Get app by ID |
| `GET` | `/api/status-apps/slug/{slug}` | Get app by public slug |
| `POST` | `/api/status-apps` | Create an app |
| `PUT` | `/api/status-apps/{id}` | Update an app |
| `DELETE` | `/api/status-apps/{id}` | Delete an app |
| `PATCH` | `/api/status-apps/{id}/status` | Update app status |

**Query params:** `platformId`, `organizationId`

---

## 78. Component Endpoints (`/api/components`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/components` | List components |
| `GET` | `/api/components/{id}` | Get component |
| `POST` | `/api/components` | Create component |
| `PUT` | `/api/components/{id}` | Update component |
| `DELETE` | `/api/components/{id}` | Delete component |
| `PUT` | `/api/components/reorder` | Bulk reorder (body: `[{id, displayOrder}]`) |

**Query params:** `appId`

---

## 79. Incident Endpoints (`/api/incidents`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/incidents` | List incidents |
| `GET` | `/api/incidents/{id}` | Get incident |
| `POST` | `/api/incidents` | Create incident |
| `PUT` | `/api/incidents/{id}` | Update incident |
| `DELETE` | `/api/incidents/{id}` | Delete incident |
| `PATCH` | `/api/incidents/{id}/resolve` | Mark incident as resolved |
| `GET` | `/api/incidents/{id}/updates` | List timeline updates |
| `POST` | `/api/incidents/{id}/updates` | Add timeline update |
| `DELETE` | `/api/incidents/{incidentId}/updates/{updateId}` | Delete update |

---

## 80. Maintenance Endpoints (`/api/maintenance`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/maintenance` | List maintenance windows |
| `GET` | `/api/maintenance/{id}` | Get maintenance window |
| `POST` | `/api/maintenance` | Create maintenance window |
| `PUT` | `/api/maintenance/{id}` | Update maintenance window |
| `DELETE` | `/api/maintenance/{id}` | Delete maintenance window |
| `PATCH` | `/api/maintenance/{id}/start` | Mark as in-progress |
| `PATCH` | `/api/maintenance/{id}/complete` | Mark as completed |
| `GET` | `/api/maintenance/upcoming` | List upcoming windows |
| `GET` | `/api/maintenance/active` | List currently active windows |

---

## 81. Organisation Endpoints (`/api/organizations`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/organizations` | List organisations |
| `GET` | `/api/organizations/{id}` | Get organisation |
| `GET` | `/api/organizations/current` | Get current user's organisation |
| `POST` | `/api/organizations` | Create organisation |
| `PUT` | `/api/organizations/{id}` | Update organisation |
| `DELETE` | `/api/organizations/{id}` | Delete organisation |
| `PATCH` | `/api/organizations/{id}/status` | Update status |

---

## 82. User Endpoints (`/api/users`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/users` | List users |
| `GET` | `/api/users/{id}` | Get user |
| `POST` | `/api/users` | Create user |
| `PUT` | `/api/users/{id}` | Update user |
| `DELETE` | `/api/users/{id}` | Delete user |
| `PATCH` | `/api/users/{id}/password` | Change password |
| `PATCH` | `/api/users/{id}/enable` | Enable user |
| `PATCH` | `/api/users/{id}/disable` | Disable user |
| `PATCH` | `/api/users/{id}/role` | Update role |
| `GET` | `/api/users/profile` | Current user's profile |

---

## 83. Tenant Endpoints (`/api/tenants`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/tenants` | List all tenants (SUPERADMIN only) |
| `GET` | `/api/tenants/{id}` | Get tenant |
| `GET` | `/api/tenants/name/{name}` | Find tenant by name |
| `POST` | `/api/tenants` | Create tenant |
| `PUT` | `/api/tenants/{id}` | Update tenant |
| `DELETE` | `/api/tenants/{id}` | Delete tenant |

---

## 84. Health Check Endpoints (`/api/health-checks`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/health-checks` | List health check settings |
| `GET` | `/api/health-checks/{id}` | Get settings |
| `POST` | `/api/health-checks` | Create settings |
| `PUT` | `/api/health-checks/{id}` | Update settings |
| `DELETE` | `/api/health-checks/{id}` | Delete settings |
| `POST` | `/api/health-checks/trigger/app/{appId}` | Manual trigger for app |
| `POST` | `/api/health-checks/trigger/component/{componentId}` | Manual trigger for component |

---

## 85. Alert Rule Endpoints (`/api/alert-rules`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/alert-rules` | List rules |
| `GET` | `/api/alert-rules/{id}` | Get rule |
| `POST` | `/api/alert-rules` | Create rule |
| `PUT` | `/api/alert-rules/{id}` | Update rule |
| `DELETE` | `/api/alert-rules/{id}` | Delete rule |
| `PATCH` | `/api/alert-rules/{id}/toggle` | Enable/disable rule |

---

## 86. Drop Rule Endpoints (`/api/drop-rules`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/drop-rules` | List rules |
| `GET` | `/api/drop-rules/{id}` | Get rule |
| `POST` | `/api/drop-rules` | Create rule |
| `PUT` | `/api/drop-rules/{id}` | Update rule |
| `DELETE` | `/api/drop-rules/{id}` | Delete rule |
| `PATCH` | `/api/drop-rules/{id}/toggle` | Enable/disable rule |

---

## 87. Notification Subscriber Endpoints (`/api/notification-subscribers`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/notification-subscribers` | List subscribers |
| `GET` | `/api/notification-subscribers/{id}` | Get subscriber |
| `GET` | `/api/notification-subscribers/app/{appId}` | Subscribers for an app |
| `GET` | `/api/notification-subscribers/app/{appId}/count` | Subscriber count |
| `POST` | `/api/notification-subscribers` | Subscribe |
| `DELETE` | `/api/notification-subscribers/{id}` | Unsubscribe |

---

## 88. Log Endpoints (`/api/logs`)

### Public (no auth, uses `X-Api-Key` header)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/logs` | Ingest a single log entry |
| `POST` | `/api/logs/batch` | Ingest a batch of log entries |

**Single log request body:**
```json
{
  "serviceName": "payment-service",
  "severity": "ERROR",
  "message": "Payment processing failed",
  "timestamp": 1711598400000,
  "traceId": "abc123",
  "spanId": "def456",
  "metadata": { "orderId": "ORD-001" }
}
```

**Batch request body:**
```json
{
  "logs": [
    { ... },
    { ... }
  ]
}
```

### Authenticated

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/logs` | List logs (paginated, filterable) |
| `GET` | `/api/logs/services` | Distinct service names in the organisation |

**Log query params:** `serviceName`, `severity`, `from` (epoch ms), `to` (epoch ms), `search` (message text)

---

## 89. Log Metrics Endpoints (`/api/log-metrics`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/log-metrics` | List metrics with filters |

**Query params:** `serviceName`, `severity`, `from` (ISO date), `to` (ISO date), `bucketSize` (minutes)

---

## 90. Log API Key Endpoints (`/api/log-api-keys`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/log-api-keys` | List keys |
| `GET` | `/api/log-api-keys/{id}` | Get key |
| `POST` | `/api/log-api-keys` | Generate new key (returns plain key once) |
| `PUT` | `/api/log-api-keys/{id}` | Update key metadata |
| `DELETE` | `/api/log-api-keys/{id}` | Delete key |
| `PATCH` | `/api/log-api-keys/{id}/toggle` | Enable/disable key |

---

## 91. Platform Event Endpoints (`/api/events`)

### Public (no auth, uses `X-Api-Key`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/events/log` | Post an event from an external system |

**Request body:**
```json
{
  "eventType": "deploy",
  "message": "Deployed v2.3.1 to production",
  "componentId": "<uuid>",
  "appId": "<uuid>"
}
```

### Authenticated

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/events` | List events |
| `GET` | `/api/events/{id}` | Get event |
| `POST` | `/api/events` | Create event (admin) |
| `PUT` | `/api/events/{id}` | Update event |
| `DELETE` | `/api/events/{id}` | Delete event |
| `POST` | `/api/events/{id}/regenerate-key` | Regenerate the API key for this event source |

---

## 92. Uptime History Endpoints (`/api/uptime-history`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/uptime-history` | List uptime history |
| `POST` | `/api/uptime-history/backfill` | Backfill history for a date range |
| `POST` | `/api/uptime-history/calculate` | Calculate uptime for a specific app/date |
| `POST` | `/api/uptime-history/trigger-daily` | Trigger the daily uptime calculation job now |

---

## 93. Process Mining Endpoints (`/api/logs/process-mining`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/logs/process-mining` | Get process mining analysis data |

**Query params:**
- `serviceName` — filter to a specific service
- `from` / `to` — epoch ms time range
- `traceField` — which log field to use as the trace/case identifier (default: `traceId`)

**Response:**
```json
{
  "traces": [
    {
      "traceId": "abc123",
      "events": [
        { "timestamp": ..., "message": "...", "severity": "INFO" }
      ]
    }
  ],
  "totalTraces": 42,
  "averageEventsPerTrace": 5.2
}
```

---

## 94. Public Status Endpoints (`/api/public/status`)

All public endpoints require **no authentication** and return only publicly visible data.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/public/status/{slug}` | Full status summary for a status app |
| `GET` | `/api/public/status/{slug}/components` | Components with current status |
| `GET` | `/api/public/status/{slug}/incidents` | Active and recent incidents |
| `GET` | `/api/public/status/{slug}/incidents/{id}` | Incident detail with updates |
| `GET` | `/api/public/status/{slug}/maintenance` | Upcoming and active maintenance |
| `GET` | `/api/public/status/{slug}/uptime` | 90-day uptime history |
| `GET` | `/api/public/status/{slug}/history` | Historical incident list |

These endpoints are consumed by the public-facing Thymeleaf templates and can also be embedded by external consumers.

---

## 95. Pagination Reference

All list endpoints support Spring Data pagination:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | `0` | Zero-based page index |
| `size` | Integer | `20` | Page size (max typically 100) |
| `sort` | String | `createdDate,desc` | Sort field and direction |

Example: `GET /api/logs?page=2&size=50&sort=timestamp,desc&serviceName=api`

Paginated responses use the Spring Data `Page<T>` envelope format (see item 74).

---

## 96. Standard Error Response Format

All errors return a consistent JSON envelope:

```json
{
  "timestamp": "2026-03-28T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/status-platforms",
  "errors": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

| HTTP Status | When used |
|------------|-----------|
| `200 OK` | Successful GET, PUT, PATCH |
| `201 Created` | Successful POST |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation failure, malformed JSON |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | Authenticated but insufficient role |
| `404 Not Found` | Entity not found |
| `409 Conflict` | Duplicate unique field (e.g., slug already exists) |
| `500 Internal Server Error` | Unhandled exception |

---

## 97. Swagger / OpenAPI Access

**OpenAPI JSON spec:** `GET /v3/api-docs`

**Swagger UI:** Disabled by default (`springdoc.swagger-ui.enabled=false`). Enable for development:
```properties
springdoc.swagger-ui.enabled=true
```
Then access at: `http://localhost:8383/swagger-ui.html`

**Import to Postman:**
1. Open Postman → Import
2. Paste URL: `http://localhost:8383/v3/api-docs`
3. Postman auto-generates a collection with all endpoints

**Import to Insomnia:**
1. Application → Import/Export → Import Data
2. From URL: `http://localhost:8383/v3/api-docs`
