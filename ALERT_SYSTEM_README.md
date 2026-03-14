# Alert System Documentation

## Overview
Complete warehouse alert system with automated monitoring, resolution tracking, and audit trail.

## Features
✅ **Automated Alert Generation** - Scheduled checks every hour for:
- Low stock (≤10 units)
- Expiring batches (within 30 days)
- Expired batches (past expiry date)

✅ **Alert Management** - Full CRUD operations with filtering by type, severity, status

✅ **Resolution Workflow** - Mark alerts as resolved with optional comments and user tracking

✅ **Audit Trail** - Complete history of all alert state changes

✅ **Dashboard Statistics** - Real-time metrics for all alert types

## Alert Types
| Type | Description | Generated When |
|------|-------------|----------------|
| `LOW_STOCK` | Stock level critical | Batch quantity ≤ 10 units |
| `EXPIRING_SOON` | Batch expiring soon | Expiry date within 30 days |
| `EXPIRED` | Batch expired | Expiry date in the past |
| `SYSTEM` | System notifications | Manual creation only |

## Severity Levels
| Severity | Usage |
|----------|-------|
| `CRITICAL` | Stock ≤ 5 units OR expired batches |
| `HIGH` | Stock ≤ 10 units (but > 5) |
| `MEDIUM` | Expiring within 30 days |
| `LOW` | System notifications |

## Alert Status
| Status | Description |
|--------|-------------|
| `OPEN` | New alert, not yet addressed |
| `IN_PROGRESS` | Being actively handled |
| `RESOLVED` | Issue resolved and closed |

## API Endpoints

### 1. Get All Alerts
```http
GET /alerts
```
Returns all alerts with full details including batch, medicine, warehouse info.

**Response Example:**
```json
[
  {
    "alertId": 1,
    "alertType": "LOW_STOCK",
    "severity": "HIGH",
    "status": "OPEN",
    "message": "Low stock: Paracetamol 500mg",
    "description": "Stock level is 8 units",
    "createdAt": "2026-02-25T10:00:00",
    "resolvedAt": null,
    "batch": {
      "batchId": 1,
      "lotNumber": "LOT001",
      "quantity": 8,
      "expiryDate": "2027-05-15"
    },
    "medicine": {
      "medicineId": 1,
      "name": "Paracetamol 500mg",
      "form": "Tablet"
    },
    "warehouse": {
      "warehouseId": 1,
      "name": "Main Warehouse"
    },
    "resolvedBy": null
  }
]
```

### 2. Get Active Alerts
```http
GET /alerts/active
```
Returns only OPEN and IN_PROGRESS alerts (excludes RESOLVED).

### 3. Get Alert by ID
```http
GET /alerts/{id}
```
Returns single alert with full details.

### 4. Filter by Type
```http
GET /alerts/type/{type}
```
**Valid types:** `LOW_STOCK`, `EXPIRING_SOON`, `EXPIRED`, `SYSTEM`

Example:
```http
GET /alerts/type/LOW_STOCK
```

### 5. Filter by Severity
```http
GET /alerts/severity/{severity}
```
**Valid severities:** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

Example:
```http
GET /alerts/severity/CRITICAL
```

### 6. Filter by Status
```http
GET /alerts/status/{status}
```
**Valid statuses:** `OPEN`, `IN_PROGRESS`, `RESOLVED`

Example:
```http
GET /alerts/status/OPEN
```

### 7. Get Dashboard Statistics
```http
GET /alerts/stats
```
Returns counts of active alerts by type.

**Response Example:**
```json
{
  "lowStockCount": 5,
  "expiringSoonCount": 8,
  "expiredCount": 2,
  "systemWarningsCount": 0,
  "totalActiveAlerts": 15
}
```

### 8. Resolve Alert
```http
POST /alerts/{id}/resolve
Content-Type: application/json

{
  "comment": "Restocked from supplier, issue resolved"
}
```
Marks alert as RESOLVED, sets resolvedAt timestamp, tracks resolvedBy user.

**Comment is optional**, can be empty string or null.

### 9. Update Alert Status
```http
PATCH /alerts/{id}/status
Content-Type: application/json

"IN_PROGRESS"
```
Updates status to OPEN, IN_PROGRESS, or RESOLVED. Creates history entry.

**Valid body values:** `"OPEN"`, `"IN_PROGRESS"`, `"RESOLVED"`

### 10. Get Alert History
```http
GET /alerts/{id}/history
```
Returns complete audit trail for an alert.

**Response Example:**
```json
[
  {
    "historyId": 1,
    "alertId": 1,
    "action": "CREATED",
    "oldStatus": null,
    "newStatus": "OPEN",
    "comment": "Auto-generated alert",
    "timestamp": "2026-02-25T10:00:00",
    "user": null
  },
  {
    "historyId": 2,
    "alertId": 1,
    "action": "STATUS_CHANGED",
    "oldStatus": "OPEN",
    "newStatus": "IN_PROGRESS",
    "comment": null,
    "timestamp": "2026-02-25T11:30:00",
    "user": {
      "userId": 1,
      "username": "admin",
      "fullName": "Administrator"
    }
  },
  {
    "historyId": 3,
    "alertId": 1,
    "action": "RESOLVED",
    "oldStatus": "IN_PROGRESS",
    "newStatus": "RESOLVED",
    "comment": "Restocked from supplier",
    "timestamp": "2026-02-25T14:00:00",
    "user": {
      "userId": 1,
      "username": "admin",
      "fullName": "Administrator"
    }
  }
]
```

### 11. Manual Alert Check
```http
POST /alerts/check
```
Manually triggers automated alert generation (normally runs every hour).

Useful for testing without waiting for the scheduled job.

**Response:** `"Alert check completed"`

## Automated Monitoring

### Schedule
Alert checks run automatically **every hour** using Spring's `@Scheduled` annotation:
```java
@Scheduled(cron = "0 0 * * * *") // At minute 0 of every hour
public void checkAndGenerateAlerts()
```

### Duplicate Prevention
System prevents duplicate alerts for the same batch and alert type.

**Logic:**
- Before creating LOW_STOCK alert for Batch #5, checks if active LOW_STOCK alert already exists for Batch #5
- If exists, skips creation
- If resolved or doesn't exist, creates new alert

### Alert Generation Logic

#### 1. Low Stock Check
```java
For each batch:
  if quantity <= 5:
    severity = CRITICAL
  else if quantity <= 10:
    severity = HIGH
  
  if no active LOW_STOCK alert for this batch:
    create alert
```

#### 2. Expiring Soon Check
```java
For each batch:
  daysUntilExpiry = expiryDate - today
  
  if 0 < daysUntilExpiry <= 30:
    severity = MEDIUM
    
    if no active EXPIRING_SOON alert for this batch:
      create alert
```

#### 3. Expired Check
```java
For each batch:
  if expiryDate < today:
    severity = CRITICAL
    
    if no active EXPIRED alert for this batch:
      create alert
```

## Database Schema

### Alert Table
```sql
CREATE TABLE alert (
  alert_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_type VARCHAR(50) NOT NULL,
  severity VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  message VARCHAR(500) NOT NULL,
  description TEXT,
  created_at TIMESTAMP NOT NULL,
  resolved_at TIMESTAMP,
  batch_id BIGINT,
  medicine_id BIGINT,
  warehouse_id BIGINT,
  resolved_by_user_id BIGINT,
  FOREIGN KEY (batch_id) REFERENCES batch(batch_id),
  FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id),
  FOREIGN KEY (warehouse_id) REFERENCES warehouse(warehouse_id),
  FOREIGN KEY (resolved_by_user_id) REFERENCES users(user_id)
);
```

### Alert History Table
```sql
CREATE TABLE alert_history (
  history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_id BIGINT NOT NULL,
  action VARCHAR(50) NOT NULL,
  old_status VARCHAR(50),
  new_status VARCHAR(50),
  comment TEXT,
  timestamp TIMESTAMP NOT NULL,
  user_id BIGINT,
  FOREIGN KEY (alert_id) REFERENCES alert(alert_id),
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

## Setup Instructions

### 1. Enable Scheduling
Already configured in `WarehouseApplication.java`:
```java
@SpringBootApplication
@EnableScheduling
public class WarehouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseApplication.class, args);
    }
}
```

### 2. Database Setup
Run the SQL scripts in order:
```bash
# 1. Create tables (auto-created by JPA on first run)
# 2. Insert sample data
mysql -u root -p warehouse_db < sample_alerts_data.sql
```

### 3. Frontend Proxy Configuration
Already configured in `vite.config.ts`:
```javascript
server: {
  proxy: {
    '/alerts': {
      target: 'http://localhost:9090',
      changeOrigin: true
    }
  }
}
```

### 4. Frontend Service Integration
Import and use from `alerts.ts`:
```typescript
import { 
  getAlertStats, 
  getActiveAlerts, 
  resolveAlert 
} from '@/services/alerts';

// Get stats
const stats = await getAlertStats();

// Get active alerts
const alerts = await getActiveAlerts();

// Resolve alert
await resolveAlert(alertId, 'Issue fixed');
```

## Testing with Postman

1. **Import Collection**: `Warehouse_Alert_API.postman_collection.json`

2. **Login First**: 
   - Run "Auth > Login" request
   - Token auto-saved to collection variable

3. **Test Alert Flow**:
   ```
   a. POST /alerts/check           → Generate alerts
   b. GET /alerts/active           → View generated alerts
   c. GET /alerts/stats            → Check dashboard stats
   d. PATCH /alerts/1/status       → Mark as IN_PROGRESS
   e. POST /alerts/1/resolve       → Resolve with comment
   f. GET /alerts/1/history        → View audit trail
   ```

## Frontend Integration Examples

### Update AlertsStatsGrid Component
```typescript
import { useEffect, useState } from 'react';
import { getAlertStats } from '@/services/alerts';

export default function AlertsStatsGrid() {
  const [stats, setStats] = useState({
    lowStockCount: 0,
    expiringSoonCount: 0,
    expiredCount: 0,
    systemWarningsCount: 0
  });

  useEffect(() => {
    const fetchStats = async () => {
      const data = await getAlertStats();
      setStats(data);
    };
    fetchStats();
  }, []);

  return (
    // Render stats using stats.lowStockCount, stats.expiringSoonCount, etc.
  );
}
```

### Update AlertsTable Component
```typescript
import { useEffect, useState } from 'react';
import { getActiveAlerts, resolveAlert } from '@/services/alerts';
import type { Alert } from '@/services/alerts';

export default function AlertsTable() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const data = await getActiveAlerts();
      setAlerts(data);
    } catch (error) {
      console.error('Failed to fetch alerts:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const handleResolve = async (alertId: number) => {
    try {
      await resolveAlert(alertId, 'Resolved from UI');
      await fetchAlerts(); // Refresh table
    } catch (error) {
      console.error('Failed to resolve alert:', error);
    }
  };

  return (
    // Render table with alerts data
  );
}
```

## Monitoring & Maintenance

### Logs to Monitor
```java
// Service logs (AlertService.java)
log.info("Starting automated alert check...");
log.info("Low stock check completed. Alerts created: {}", alertsCreated);
log.info("Expiring batches check completed. Alerts created: {}", alertsCreated);
log.info("Expired batches check completed. Alerts created: {}", alertsCreated);
```

### Performance Considerations
- Each scheduled run queries all batches (typically < 1000)
- Duplicate prevention uses indexed queries
- Consider archiving resolved alerts after 90 days

### Customization
To change thresholds, modify `AlertService.java`:
```java
// Current thresholds
private static final int LOW_STOCK_THRESHOLD = 10;
private static final int CRITICAL_STOCK_THRESHOLD = 5;
private static final int EXPIRING_DAYS_THRESHOLD = 30;

// Modify these values as needed
```

To change schedule frequency:
```java
// Current: Every hour (0 0 * * * *)
@Scheduled(cron = "0 0 * * * *")

// Every 30 minutes: 0 0/30 * * * *
// Every day at 8 AM: 0 0 8 * * *
// Every 6 hours: 0 0 */6 * * *
```

## Troubleshooting

### Alerts Not Being Generated
1. Check `@EnableScheduling` is present in `WarehouseApplication.java`
2. Verify scheduler logs in console
3. Use `POST /alerts/check` to test manually
4. Check batch data has valid quantities and expiry dates

### Duplicate Alerts Created
- Should not happen due to duplicate prevention logic
- If occurs, check `findActiveAlertByBatchAndType` query
- Verify batch_id foreign keys are correct

### Frontend Not Showing Data
1. Check browser console for errors
2. Verify Vite proxy is configured for `/alerts`
3. Check JWT token is valid (re-login if expired)
4. Verify backend is running on port 9090

## Security Notes
- All endpoints require JWT authentication
- Resolved alerts track which user performed the action
- Alert history maintains complete audit trail
- No delete endpoint provided (maintain immutable history)

## Next Steps
1. ✅ Backend complete with automated monitoring
2. ⏳ Connect frontend components to real API
3. ⏳ Add realtime polling or WebSocket for live updates
4. ⏳ Implement notification system (email/SMS for critical alerts)
5. ⏳ Add alert assignment to specific users
6. ⏳ Dashboard charts showing alert trends over time
