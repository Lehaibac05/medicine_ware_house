import { Button, Space, Tag, Typography, message, Flex, Input, Modal, Select } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState, useMemo } from 'react'
import { ReloadOutlined } from '@ant-design/icons'
import BaseTable from '../../../components/base/BaseTable'
import { type Alert, getAllAlerts, resolveAlert, checkAndGenerateAlerts, updateAlertStatus } from '../../../services/alerts'
import type { AlertFilters } from '../AlertsPage'
import dayjs from 'dayjs'
import { hasAnyRole } from '../../../utils/auth'

const { Text } = Typography;

export type AlertRow = {
  key: string;
  alertId: number;
  alertType: string;
  medicineName: string;
  warehouse: string;
  date: string;
  createdAtRaw: string;
  severity: string;
  status: string;
  message: string;
  description: string;
};

const severityTag = (value: string) => {
  const upper = value.toUpperCase();
  if (upper === "CRITICAL") return <Tag color="red">Nghiêm trọng</Tag>;
  if (upper === "HIGH") return <Tag color="volcano">Cao</Tag>;
  if (upper === "MEDIUM") return <Tag color="gold">Trung bình</Tag>;
  return <Tag>Thấp</Tag>;
};

const statusTag = (value: string) => {
  const upper = value.toUpperCase();
  if (upper === "RESOLVED") return <Tag color="green">Đã xử lý</Tag>;
  if (upper === "IN_PROGRESS") return <Tag color="blue">Đang xử lý</Tag>;
  return <Tag color="default">Mở</Tag>;
};

const typeTag = (value: string) => {
  const upper = value.toUpperCase();
  if (upper === "EXPIRED") return <Tag color="red">Đã hết hạn</Tag>;
  if (upper === "EXPIRING_SOON") return <Tag color="gold">Sắp hết hạn</Tag>;
  if (upper === "LOW_STOCK") return <Tag color="volcano">Tồn kho thấp</Tag>;
  return <Tag color="geekblue">Hệ thống</Tag>;
};

const toTypeLabel = (value: string) => {
  const upper = value.toUpperCase();
  if (upper === "EXPIRED") return "Đã hết hạn";
  if (upper === "EXPIRING_SOON") return "Sắp hết hạn";
  if (upper === "LOW_STOCK") return "Tồn kho thấp";
  return "Hệ thống";
};

const toSeverityLabel = (value: string) => {
  const upper = value.toUpperCase();
  if (upper === "CRITICAL") return "Nghiêm trọng";
  if (upper === "HIGH") return "Cao";
  if (upper === "MEDIUM") return "Trung bình";
  return "Thấp";
};

const toStatusLabel = (value: string) => {
  const upper = value.toUpperCase();
  if (upper === "RESOLVED") return "Đã xử lý";
  if (upper === "IN_PROGRESS") return "Đang xử lý";
  return "Mở";
};

type AlertsTableProps = {
  filters: AlertFilters;
  onSearch: (value: string) => void;
};

function AlertsTable({ filters, onSearch }: AlertsTableProps) {
  const [messageApi, contextHolder] = message.useMessage();
  const [alerts, setAlerts] = useState<AlertRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [scanning, setScanning] = useState(false);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [detailTarget, setDetailTarget] = useState<AlertRow | null>(null);
  const [quickStatusFilter, setQuickStatusFilter] = useState<"all" | "open" | "resolved">("all");
  const [expiredModalOpen, setExpiredModalOpen] = useState(false);
  const [expiredTarget, setExpiredTarget] = useState<AlertRow | null>(null);
  const [expiredAction, setExpiredAction] = useState<string | undefined>();
  const [expiredNote, setExpiredNote] = useState("");
  const canTriggerScan = hasAnyRole(["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]);
  const canResolve = hasAnyRole(["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]);
  const canUpdateProgress = hasAnyRole(["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]);

  async function handleScanAlerts() {
    try {
      setScanning(true);
      const result = await checkAndGenerateAlerts();
      messageApi.success(result?.message || "Quét cảnh báo thành công");
      await loadAlerts();
    } catch (error) {
      console.error("Scan alerts error:", error);
      messageApi.error("Không thể quét cảnh báo");
    } finally {
      setScanning(false);
    }
  };

  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Danh sách cảnh báo
        </Text>
      </div>
      <div className="flex items-center gap-2">
        <Button
          type={quickStatusFilter === "all" ? "primary" : "default"}
          onClick={() => setQuickStatusFilter("all")}
        >
          Tất cả
        </Button>
        <Button
          type={quickStatusFilter === "open" ? "primary" : "default"}
          onClick={() => setQuickStatusFilter("open")}
        >
          Đang mở
        </Button>
        <Button
          type={quickStatusFilter === "resolved" ? "primary" : "default"}
          onClick={() => setQuickStatusFilter("resolved")}
        >
          Đã xử lý
        </Button>
        {canTriggerScan && (
          <Button
            icon={<ReloadOutlined />}
            loading={scanning}
            onClick={handleScanAlerts}
          >
            Scan cảnh báo
          </Button>
        )}
        <Input.Search
          placeholder="Tìm theo mã cảnh báo..."
          className="w-[320px]"
          allowClear
          onSearch={onSearch}
          onChange={(e) => !e.target.value && onSearch("")}
        />
      </div>
    </Flex>
  );
  useEffect(() => {
    initializeAlerts();
  }, []);

  const initializeAlerts = async () => {
    try {
      setLoading(true);

      await loadAlerts();
    } catch (error) {
      console.error("❌ Initialize alerts error:", error);
      messageApi.error("Không thể tải cảnh báo");
    } finally {
      setLoading(false);
    }
  };

  const loadAlerts = async () => {
    try {
      console.log("📊 Loading alerts...");
      const data = await getAllAlerts();
      console.log("✅ Loaded alerts:", data.length);

      const mapped: AlertRow[] = data.map((alert: Alert) => ({
        key: alert.alertId.toString(),
        alertId: alert.alertId,
        alertType: alert.alertType,
        medicineName: alert.medicineName || "—",
        warehouse: alert.warehouseName || "—",
        date: new Date(alert.createdAt).toLocaleDateString("vi-VN"),
        createdAtRaw: alert.createdAt,
        severity: alert.severity,
        status: alert.status,
        message: alert.message,
        description: alert.description || "",
      }));

      setAlerts(mapped);
    } catch (error) {
      messageApi.error("Không thể tải cảnh báo");
      console.error("Load alerts error:", error);
    } finally {
      setLoading(false);
    }
  };

  const filteredAndSortedAlerts = useMemo(() => {
    let result = [...alerts];

    if (quickStatusFilter === "open") {
      result = result.filter((alert) => alert.status.toUpperCase() !== "RESOLVED");
    }
    if (quickStatusFilter === "resolved") {
      result = result.filter((alert) => alert.status.toUpperCase() === "RESOLVED");
    }

    // Filter by alert type
    if (filters.alertType !== "all") {
      result = result.filter((alert) => alert.alertType === filters.alertType);
    }

    // Filter by severity
    if (filters.severity !== "all") {
      result = result.filter(
        (alert) => alert.severity.toUpperCase() === filters.severity,
      );
    }

    // Filter by date range
    if (filters.dateRange && filters.dateRange[0] && filters.dateRange[1]) {
      const startDate = filters.dateRange[0].startOf("day");
      const endDate = filters.dateRange[1].endOf("day");

      result = result.filter((alert) => {
        const alertDate = dayjs(alert.date, "DD/MM/YYYY");
        return alertDate.isAfter(startDate) && alertDate.isBefore(endDate);
      });
    }

    // Filter by search text
    if (filters.searchText) {
      const searchLower = filters.searchText.toLowerCase();
      result = result.filter(
        (alert) =>
          alert.alertId.toString().includes(searchLower) ||
          alert.medicineName.toLowerCase().includes(searchLower) ||
          alert.warehouse.toLowerCase().includes(searchLower) ||
          alert.message.toLowerCase().includes(searchLower),
      );
    }

    // Sort
    const severityOrder = { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };

    if (filters.sortBy === "date-desc") {
      result.sort((a, b) => {
        const dateA = dayjs(a.date, "DD/MM/YYYY");
        const dateB = dayjs(b.date, "DD/MM/YYYY");
        return dateB.diff(dateA);
      });
    } else if (filters.sortBy === "date-asc") {
      result.sort((a, b) => {
        const dateA = dayjs(a.date, "DD/MM/YYYY");
        const dateB = dayjs(b.date, "DD/MM/YYYY");
        return dateA.diff(dateB);
      });
    } else if (filters.sortBy === "severity-desc") {
      result.sort((a, b) => {
        const severityA =
          severityOrder[
            a.severity.toUpperCase() as keyof typeof severityOrder
          ] || 0;
        const severityB =
          severityOrder[
            b.severity.toUpperCase() as keyof typeof severityOrder
          ] || 0;
        return severityB - severityA;
      });
    }

    return result;
  }, [alerts, filters, quickStatusFilter]);

  const openDetailModal = (record: AlertRow) => {
    setDetailTarget(record);
    setDetailModalOpen(true);
  };

  const handleResolve = async (alertId: number) => {
    try {
      setResolvingId(alertId);
      await resolveAlert(alertId, "Đã xử lý từ bảng cảnh báo");
      messageApi.success("Đã xử lý cảnh báo thành công");
      await loadAlerts(); // Reload data
    } catch (error) {
      messageApi.error("Xử lý cảnh báo thất bại");
      console.error("Resolve alert error:", error);
    } finally {
      setResolvingId(null);
    }
  };

  const handleMarkInProgress = async (alertId: number) => {
    try {
      setUpdatingId(alertId);
      await updateAlertStatus(alertId, "IN_PROGRESS");
      messageApi.success("Đã cập nhật cảnh báo sang trạng thái đang xử lý");
      await loadAlerts();
    } catch (error) {
      messageApi.error("Không thể cập nhật trạng thái cảnh báo");
      console.error("Update alert status error:", error);
    } finally {
      setUpdatingId(null);
    }
  };

  const openExpiredResolveModal = (record: AlertRow) => {
    setExpiredTarget(record);
    setExpiredAction(undefined);
    setExpiredNote("");
    setExpiredModalOpen(true);
  };

  const handleResolveExpired = async () => {
    if (!expiredTarget) return;
    if (!expiredAction) {
      messageApi.warning("Vui lòng chọn hướng xử lý cho cảnh báo đã hết hạn");
      return;
    }

    const note = expiredNote.trim();
    if (note.length < 10) {
      messageApi.warning("Ghi chú xử lý phải có tối thiểu 10 ký tự");
      return;
    }

    try {
      setResolvingId(expiredTarget.alertId);
      const comment = `Hướng xử lý: ${expiredAction}. Ghi chú: ${note}`;
      await resolveAlert(expiredTarget.alertId, comment);
      messageApi.success("Đã xử lý cảnh báo hết hạn thành công");
      setExpiredModalOpen(false);
      setExpiredTarget(null);
      await loadAlerts();
    } catch (error) {
      messageApi.error("Xử lý cảnh báo hết hạn thất bại");
      console.error("Resolve expired alert error:", error);
    } finally {
      setResolvingId(null);
    }
  };

  const columns: ColumnsType<AlertRow> = [
    {
      title: "Mã cảnh báo",
      dataIndex: "alertId",
      key: "alertId",
      render: (value: number) => (
        <Text strong>ALT-{value.toString().padStart(4, "0")}</Text>
      ),
      width: 120,
    },
    {
      title: "Loại cảnh báo",
      dataIndex: "alertType",
      key: "alertType",
      render: (value: string) => typeTag(value),
      width: 140,
    },
    {
      title: "Tên thuốc",
      dataIndex: "medicineName",
      key: "medicineName",
      ellipsis: true,
    },
    {
      title: "Kho",
      dataIndex: "warehouse",
      key: "warehouse",
      width: 160,
    },
    {
      title: "Ngày",
      dataIndex: "date",
      key: "date",
      width: 120,
    },
    {
      title: "Mức độ",
      dataIndex: "severity",
      key: "severity",
      render: (value: string) => severityTag(value),
      width: 120,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (value: string) => statusTag(value),
      width: 140,
    },
    {
      title: "Thao tác",
      key: "actions",
      width: 180,
      render: (_: unknown, record: AlertRow) => (
        <Space>
          <Button size="small" onClick={() => openDetailModal(record)}>
            Xem
          </Button>
          {canUpdateProgress && record.alertType !== "EXPIRED" && record.status.toUpperCase() === "OPEN" && (
            <Button
              size="small"
              loading={updatingId === record.alertId}
              onClick={() => handleMarkInProgress(record.alertId)}
            >
              Nhận xử lý
            </Button>
          )}
          {canResolve && record.status.toUpperCase() !== "RESOLVED" && record.alertType !== "EXPIRED" && (
            <Button
              size="small"
              type="primary"
              loading={resolvingId === record.alertId}
              onClick={() => handleResolve(record.alertId)}
            >
              Đóng cảnh báo
            </Button>
          )}
          {canResolve && record.status.toUpperCase() !== "RESOLVED" && record.alertType === "EXPIRED" && (
            <Button
              size="small"
              type="primary"
              danger
              loading={resolvingId === record.alertId}
              onClick={() => openExpiredResolveModal(record)}
            >
              Xử lý hết hạn
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      {contextHolder}
      <BaseTable
        title={() => tableHeader}
        columns={columns}
        dataSource={filteredAndSortedAlerts}
        loading={loading}
        scroll={{ x: 980 }}
        cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
      />
      <Modal
        title="Chi tiết cảnh báo"
        open={detailModalOpen}
        onCancel={() => {
          setDetailModalOpen(false);
          setDetailTarget(null);
        }}
        footer={[
          <Button
            key="close"
            onClick={() => {
              setDetailModalOpen(false);
              setDetailTarget(null);
            }}
          >
            Đóng
          </Button>,
        ]}
      >
        <div className="flex flex-col gap-2">
          <Text><strong>Mã cảnh báo:</strong> {detailTarget ? `ALT-${detailTarget.alertId.toString().padStart(4, "0")}` : ""}</Text>
          <Text><strong>Loại:</strong> {detailTarget ? toTypeLabel(detailTarget.alertType) : ""}</Text>
          <Text><strong>Mức độ:</strong> {detailTarget ? toSeverityLabel(detailTarget.severity) : ""}</Text>
          <Text><strong>Trạng thái:</strong> {detailTarget ? toStatusLabel(detailTarget.status) : ""}</Text>
          <Text><strong>Tên thuốc:</strong> {detailTarget ? detailTarget.medicineName : ""}</Text>
          <Text><strong>Kho:</strong> {detailTarget ? detailTarget.warehouse : ""}</Text>
          <Text><strong>Thời gian tạo:</strong> {detailTarget ? new Date(detailTarget.createdAtRaw).toLocaleString("vi-VN") : ""}</Text>
          <Text><strong>Nội dung:</strong> {detailTarget ? detailTarget.message : ""}</Text>
          <Text><strong>Mô tả:</strong> {detailTarget?.description?.trim() || "Không có"}</Text>
        </div>
      </Modal>
      <Modal
        title="Xử lý cảnh báo đã hết hạn"
        open={expiredModalOpen}
        onCancel={() => {
          setExpiredModalOpen(false);
          setExpiredTarget(null);
        }}
        onOk={() => void handleResolveExpired()}
        okText="Xác nhận xử lý"
        cancelText="Hủy"
        confirmLoading={expiredTarget ? resolvingId === expiredTarget.alertId : false}
      >
        <div className="flex flex-col gap-3">
          <Text className="text-sm text-slate-600">
            Cảnh báo {expiredTarget ? `ALT-${expiredTarget.alertId.toString().padStart(4, "0")}` : ""} yêu cầu ghi nhận hướng xử lý thủ công.
          </Text>
          <Select
            placeholder="Chọn hướng xử lý"
            value={expiredAction}
            onChange={setExpiredAction}
            options={[
              { value: "Hủy lô thuốc", label: "Hủy lô thuốc" },
              { value: "Trả nhà cung cấp", label: "Trả nhà cung cấp" },
              { value: "Giữ lại để kiểm kê", label: "Giữ lại để kiểm kê" },
              { value: "Khác", label: "Khác" },
            ]}
          />
          <Input.TextArea
            rows={4}
            value={expiredNote}
            onChange={(event) => setExpiredNote(event.target.value)}
            placeholder="Nhập ghi chú xử lý (tối thiểu 10 ký tự)"
          />
        </div>
      </Modal>
    </>
  );
}

export default AlertsTable;
