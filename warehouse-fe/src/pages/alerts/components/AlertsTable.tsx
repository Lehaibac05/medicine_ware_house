import { Button, Space, Tag, Typography, message, Flex, Input } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useState, useMemo } from "react";
import BaseTable from "../../../components/base/BaseTable";
import {
  type Alert,
  getAllAlerts,
  resolveAlert,
  checkAndGenerateAlerts,
} from "../../../services/alerts";
import type { AlertFilters } from "../AlertsPage";
import dayjs from "dayjs";

const { Text } = Typography;

export type AlertRow = {
  key: string;
  alertId: number;
  alertType: string;
  medicineName: string;
  warehouse: string;
  date: string;
  severity: string;
  status: string;
  message: string;
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
  return <Tag color="default">Đang mở</Tag>;
};

const typeTag = (value: string) => {
  const upper = value.toUpperCase();
  if (upper === "EXPIRED") return <Tag color="red">Đã hết hạn</Tag>;
  if (upper === "EXPIRING_SOON") return <Tag color="gold">Sắp hết hạn</Tag>;
  if (upper === "LOW_STOCK") return <Tag color="volcano">Tồn kho thấp</Tag>;
  return <Tag color="geekblue">Hệ thống</Tag>;
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

  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Danh sách cảnh báo
        </Text>
      </div>
      <div className="w-[200px]">
        <Input.Search
          placeholder="Tìm kiếm theo ID hoặc tên thuốc..."
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

      // Tự động quét các lô để tạo/cập nhật cảnh báo
      console.log("Đang quét các lô để tạo cảnh báo...");
      try {
        const scanResult = await checkAndGenerateAlerts();
        console.log("Quét hoàn tất:", scanResult);
      } catch (scanError) {
        console.warn("Quét thất bại, tải cảnh báo hiện có:", scanError);
      }

      // Sau đó tải danh sách cảnh báo để hiển thị
      await loadAlerts();
    } catch (error) {
      console.error("Lỗi khởi tạo cảnh báo:", error);
      messageApi.error("Không thể tải cảnh báo");
    } finally {
      setLoading(false);
    }
  };

  const loadAlerts = async () => {
    try {
      console.log("Đang tải danh sách cảnh báo...");
      const data = await getAllAlerts();
      console.log("Đã tải danh sách cảnh báo:", data.length);

      const mapped: AlertRow[] = data.map((alert: Alert) => ({
        key: alert.alertId.toString(),
        alertId: alert.alertId,
        alertType: alert.alertType,
        medicineName: alert.medicineName || "—",
        warehouse: alert.warehouseName || "—",
        date: new Date(alert.createdAt).toLocaleDateString("en-GB"),
        severity: alert.severity,
        status: alert.status,
        message: alert.message,
      }));

      setAlerts(mapped);
    } catch (error) {
      messageApi.error("Lỗi khi tải danh sách cảnh báo");
      console.error("Tải danh sách cảnh báo lỗi:", error);
    } finally {
      setLoading(false);
    }
  };

  const filteredAndSortedAlerts = useMemo(() => {
    let result = [...alerts];

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
  }, [alerts, filters]);

  const handleResolve = async (alertId: number) => {
    try {
      setResolvingId(alertId);
      await resolveAlert(alertId, "Xử lý từ dashboard");
      messageApi.success("Đã xử lý cảnh báo thành công");
      await loadAlerts(); // Tải lại dữ liệu
    } catch (error) {
      messageApi.error("Không thể xử lý cảnh báo");
      console.error("Lỗi xử lý cảnh báo:", error);
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
      title: "Ngày tạo",
      dataIndex: "date",
      key: "date",
      width: 120,
    },
    {
      title: "Mức độ nghiêm trọng",
      dataIndex: "severity",
      key: "severity",
      render: (value: string) => severityTag(value),
      width: 200,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (value: string) => statusTag(value),
      width: 140,
    },
    {
      title: "Hành động",
      key: "actions",
      width: 180,
      render: (_: unknown, record: AlertRow) => (
        <Space>
          <Button size="small" onClick={() => messageApi.info(record.message)}>
            Xem
          </Button>
          {record.status.toUpperCase() !== "RESOLVED" && (
            <Button
              size="small"
              type="primary"
              loading={resolvingId === record.alertId}
              onClick={() => handleResolve(record.alertId)}
            >
              Xử lý
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
        cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
      />
    </>
  );
}

export default AlertsTable;
