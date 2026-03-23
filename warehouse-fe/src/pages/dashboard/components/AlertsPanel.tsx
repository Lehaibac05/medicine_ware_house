import {
  Badge,
  Button,
  Card,
  Empty,
  List,
  Typography,
  message,
} from "antd";
import type { BadgeProps } from "antd";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  checkAndGenerateAlerts,
  getActiveAlerts,
  type Alert,
} from "../../../services/alerts";
import {
  getLowStockInventory,
  type InventoryRow,
} from "../../../services/inventory";
import { getUserRoles } from "../../../utils/auth";

const { Text, Title } = Typography;

const toBadgeStatus = (severity?: string): BadgeProps["status"] => {
  const normalized = severity?.toUpperCase();
  if (normalized === "CRITICAL") return "error";
  if (normalized === "HIGH") return "warning";
  if (normalized === "MEDIUM") return "processing";
  return "default";
};

const canCreateRequest = (alert: Alert) => {
  const type = alert.alertType?.toUpperCase();
  return (
    (type === "LOW_STOCK" || type === "EXPIRING_SOON") &&
    Boolean(alert.medicineId)
  );
};

const severityRank = (severity?: string) => {
  const normalized = severity?.toUpperCase();
  if (normalized === "CRITICAL") return 4;
  if (normalized === "HIGH") return 3;
  if (normalized === "MEDIUM") return 2;
  if (normalized === "LOW") return 1;
  return 0;
};

const lowStockSeverity = (quantity: number) => {
  if (quantity < 10) return "CRITICAL";
  if (quantity < 20) return "HIGH";
  return "MEDIUM";
};

const buildSyntheticLowStockAlerts = (
  inventoryRows: InventoryRow[],
): Alert[] => {
  const now = new Date().toISOString();

  return inventoryRows
    .filter((row) => row.status === "LOW_STOCK")
    .map((row, idx) => ({
      alertId: -(idx + 1),
      alertType: "LOW_STOCK",
      severity: lowStockSeverity(row.totalStock),
      status: "OPEN",
      message: "Low Stock Alert",
      description: `${row.medicineName} tại ${row.warehouseName} còn ${row.totalStock} đơn vị`,
      createdAt: now,
      medicineId: row.medicineId,
      medicineName: row.medicineName,
      warehouseId: row.warehouseId,
      warehouseName: row.warehouseName,
    }));
};

const normalizeDashboardAlerts = (
  activeAlerts: Alert[],
  lowStockRows: InventoryRow[],
) => {
  const syntheticLowStock = buildSyntheticLowStockAlerts(lowStockRows);
  const nonLowStockAlerts = activeAlerts.filter(
    (alert) => alert.alertType?.toUpperCase() !== "LOW_STOCK",
  );

  const merged = [...nonLowStockAlerts, ...syntheticLowStock].sort((a, b) => {
    const severityDiff = severityRank(b.severity) - severityRank(a.severity);
    if (severityDiff !== 0) return severityDiff;

    const timeA = new Date(a.createdAt || 0).getTime();
    const timeB = new Date(b.createdAt || 0).getTime();
    return timeB - timeA;
  });

  const topAlerts = merged.slice(0, 6);
  const hasLowStock = topAlerts.some(
    (alert) => alert.alertType?.toUpperCase() === "LOW_STOCK",
  );

  if (hasLowStock) return topAlerts;

  const firstLowStock = merged.find(
    (alert) => alert.alertType?.toUpperCase() === "LOW_STOCK",
  );
  if (!firstLowStock) return topAlerts;

  if (topAlerts.length < 6) return [...topAlerts, firstLowStock];

  const replaced = [...topAlerts];
  replaced[replaced.length - 1] = firstLowStock;
  return replaced;
};

const loadAlertsAndLowStock = async () => {
  const [activeResult, lowStockResult] = await Promise.allSettled([
    getActiveAlerts(),
    getLowStockInventory(),
  ]);

  const activeAlerts =
    activeResult.status === "fulfilled" ? activeResult.value : [];
  const lowStockRows =
    lowStockResult.status === "fulfilled" ? lowStockResult.value : [];

  return { activeAlerts, lowStockRows };
};

function AlertsPanel() {
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const userRoles = getUserRoles();
  const canTriggerAlertScan = userRoles.some((role) =>
    ["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"].includes(role),
  );
  const canShowCreateRequestButton = userRoles.some((role) =>
    ["ROLE_ADMIN", "ROLE_WAREHOUSE_STAFF"].includes(role),
  );

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      if (canTriggerAlertScan) {
        try {
          await checkAndGenerateAlerts();
        } catch {
          // ignore scan failures for users without explicit alert permissions
        }
      }

      try {
        const { activeAlerts, lowStockRows } = await loadAlertsAndLowStock();
        setAlerts(normalizeDashboardAlerts(activeAlerts, lowStockRows));
      } catch {
        messageApi.error("Failed to load alerts");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [messageApi, canTriggerAlertScan]);

  const alertCountLabel = useMemo(
    () => `${alerts.length} mới`,
    [alerts.length],
  );

  return (
    <Card className="!rounded-2xl !border-0 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      {contextHolder}

      {/* Header */}
      <div className="mb-5 flex items-start justify-between gap-4">
        <div>
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            System Alerts
          </Text>

          <Title level={4} className="!m-0 !mt-1 font-semibold">
            Cảnh báo hệ thống
          </Title>
        </div>

        <div className="rounded-full bg-red-50 px-3 py-1 text-xs font-semibold text-red-600">
          {alertCountLabel}
        </div>
      </div>

      {/* Empty */}
      {!loading && alerts.length === 0 ? (
        <Empty description="Không có cảnh báo" />
      ) : null}

      {/* List */}
      <List
        dataSource={alerts}
        style={{
          maxHeight: 320,
          overflowY: "auto",
          paddingRight: 4, 
        }}
        renderItem={(item) => (
          <List.Item className="!border-0 !p-0 !pb-3 last:!pb-0">
            <div className="group w-full rounded-2xl border border-slate-200 bg-white p-4 transition hover:shadow-md hover:-translate-y-[1px]">
              {/* Top */}
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-2">
                  <Badge status={toBadgeStatus(item.severity)} />

                  <Text strong className="text-slate-900">
                    {item.message || item.alertType}
                  </Text>
                </div>

                {/* Severity tag */}
                <span
                  className={`text-xs font-semibold px-2 py-0.5 rounded-full ${item.severity === "CRITICAL"
                      ? "bg-red-50 text-red-600"
                      : item.severity === "HIGH"
                        ? "bg-amber-50 text-amber-600"
                        : "bg-slate-100 text-slate-600"
                    }`}
                >
                  {item.severity}
                </span>
              </div>

              {/* Description */}
              <Text className="mt-2 block text-[13px] text-slate-500 leading-relaxed">
                {item.description || "-"}
              </Text>

              {/* Action */}
              {canCreateRequest(item) && canShowCreateRequestButton ? (
                <div className="mt-3 flex items-center justify-between">
                  <Text className="text-xs text-slate-400">
                    Đề xuất tạo yêu cầu
                  </Text>

                  <Link
                    to={`/requests/new?medicineId=${item.medicineId}&warehouseId=${item.warehouseId || ""}&fromAlert=${item.alertId}&suggestedQty=10`}
                  >
                    <Button
                      size="small"
                      type="primary"
                      className="!rounded-full !px-4"
                    >
                      Tạo yêu cầu
                    </Button>
                  </Link>
                </div>
              ) : null}
            </div>
          </List.Item>
        )}
      />
    </Card>
  );
}

export default AlertsPanel;
