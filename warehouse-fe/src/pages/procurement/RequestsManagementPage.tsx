import {
  Button,
  Input,
  Space,
  Tag,
  Select,
  DatePicker,
  Typography,
  message,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../services/api";
import dayjs from "dayjs";
import {
  useApproveRequestMutation,
  useInventoryQuery,
  useRejectRequestMutation,
  useRequestsQuery,
} from "../../hooks/useWorkflow";
import MainLayout from "../../layouts/MainLayout";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import CreateMedicineRequestModal from "./components/CreateMedicineRequestModal";
import BaseTable from "../../components/base/BaseTable";
import { getUserRoles } from "../../utils/auth";

const { Text } = Typography;

type RequestRow = {
  key: string;
  requestId: number;
  medicine: string;
  requestedQuantity: number;
  currentStock: number;
  requiredDate: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
};

const statusTag = (status: RequestRow["status"]) => {
  if (status === "APPROVED") return <Tag color="green">ĐÃ DUYỆT</Tag>;
  if (status === "REJECTED") return <Tag color="red">TỪ CHỐI</Tag>;
  return <Tag color="gold">CHỜ DUYỆT</Tag>;
};

const normalizeStatus = (status?: string): RequestRow["status"] => {
  const normalized = status?.trim().toUpperCase();
  if (normalized === "APPROVED") return "APPROVED";
  if (normalized === "REJECTED") return "REJECTED";
  return "PENDING";
};

export default function RequestsManagementPage() {
  const [messageApi, contextHolder] = message.useMessage();

  const [search, setSearch] = useState("");

  // filter đang nhập
  const [filters, setFilters] = useState({
    medicine: "",
    status: "",
    date: "",
  });

  // filter đã apply
  const [appliedFilters, setAppliedFilters] = useState(filters);

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const [actingRequestId, setActingRequestId] = useState<number | null>(null);
  const [actingType, setActingType] = useState<"approve" | "reject" | null>(
    null,
  );

  const { data: requests = { content: [], totalElements: 0 }, isLoading, refetch } = useRequestsQuery({
    page: pagination.current - 1,
    size: pagination.pageSize,
    status: appliedFilters.status || undefined,
    medicineName: appliedFilters.medicine || undefined,
  });
  const { data: inventory = [], isLoading: isInventoryLoading } =
    useInventoryQuery();

  const approveMutation = useApproveRequestMutation();
  const rejectMutation = useRejectRequestMutation();
  const [openModal, setOpenModal] = useState(false);

  const userRoles = getUserRoles();
  const canManageRequests = userRoles.some((role) =>
    ["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"].includes(role),
  );

  // Update total when data changes
  useEffect(() => {
    if (requests && 'totalElements' in requests) {
      setPagination((prev) => ({
        ...prev,
        total: requests.totalElements,
      }));
    }
  }, [requests]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }));
  }, [appliedFilters, search]);

  const stockByMedicineWarehouse = useMemo(() => {
    return inventory.reduce<Record<string, number>>((acc, item) => {
      acc[`${item.medicineId}-${item.warehouseId}`] = item.totalStock;
      return acc;
    }, {});
  }, [inventory]);

  const rows = useMemo<RequestRow[]>(() => {
    const requestsContent = Array.isArray(requests) ? requests : (requests?.content || []);
    return requestsContent
      .map((request) => {
        const quantity = request.items.reduce(
          (sum: number, item: typeof request.items[0]) => sum + item.quantity,
          0,
        );

        const firstItem = request.items[0];
        const medicineName =
          firstItem?.medicineName ||
          `Medicine #${firstItem?.medicineId || "-"}`;

        const stockKey = `${firstItem?.medicineId || ""}-${request.warehouseId}`;

        return {
          key: String(request.requestId),
          requestId: request.requestId,
          medicine: medicineName,
          requestedQuantity: quantity,
          currentStock: stockByMedicineWarehouse[stockKey] ?? 0,
          requiredDate: request.requiredDate || "-",
          status: normalizeStatus(request.status),
        };
      });
  }, [requests, stockByMedicineWarehouse]);

  const getActionErrorMessage = (error: unknown, fallback: string) => {
    if (error instanceof ApiError && error.status === 403) {
      return "Bạn không có quyền thực hiện hành động này.";
    }
    return fallback;
  };

  const onApprove = async (requestId: number) => {
    try {
      setActingRequestId(requestId);
      setActingType("approve");
      await approveMutation.mutateAsync(requestId);
      messageApi.success("Yêu cầu đã được duyệt");
    } catch (error) {
      messageApi.error(getActionErrorMessage(error, "Lỗi khi duyệt yêu cầu"));
    } finally {
      setActingRequestId(null);
      setActingType(null);
    }
  };

  const onReject = async (requestId: number) => {
    try {
      setActingRequestId(requestId);
      setActingType("reject");
      await rejectMutation.mutateAsync(requestId);
      messageApi.success("Yêu cầu đã được từ chối");
    } catch (error) {
      messageApi.error(getActionErrorMessage(error, "Lỗi khi từ chối yêu cầu"));
    } finally {
      setActingRequestId(null);
      setActingType(null);
    }
  };

  const handleApplyFilters = () => {
    setAppliedFilters(filters);
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }));
  };

  const handleReset = () => {
    const empty = { medicine: "", status: "", date: "" };
    setFilters(empty);
    setAppliedFilters(empty);
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }));
  };

  const baseColumns: ColumnsType<RequestRow> = [
    { title: "ID", dataIndex: "requestId", width: 100 },
    { title: "Thuốc", dataIndex: "medicine" },
    { title: "Số lượng yêu cầu", dataIndex: "requestedQuantity", width: 160 },
    { title: "Tồn kho hiện tại", dataIndex: "currentStock", width: 140 },
    { title: "Ngày yêu cầu", dataIndex: "requiredDate", width: 140 },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 130,
      render: (value: RequestRow["status"]) => statusTag(value),
    },
  ];

  const actionColumn: ColumnsType<RequestRow>[number] = {
    title: "Hành động",
    width: 250,
    render: (_, record) => (
      <Space>
        <Button
          size="small"
          onClick={() => void onApprove(record.requestId)}
          loading={
            actingRequestId === record.requestId && actingType === "approve"
          }
        >
          Chấp nhận
        </Button>

        <Button
          size="small"
          danger
          onClick={() => void onReject(record.requestId)}
          loading={
            actingRequestId === record.requestId && actingType === "reject"
          }
        >
          Từ chối
        </Button>

        {record.status === "APPROVED" && (
          <Link to={`/purchase-orders/create?requestId=${record.requestId}`}>
            <Button size="small" type="primary">
              Tạo đơn nhập
            </Button>
          </Link>
        )}
      </Space>
    ),
  };

  const columns: ColumnsType<RequestRow> = canManageRequests
    ? [...baseColumns, actionColumn]
    : baseColumns;

  return (
    <MainLayout>
      {contextHolder}

      {/* FILTER */}
      <BaseFilterCard
        actions={
          <div className="flex gap-2">
            <Button className="h-[40px] flex-1" onClick={handleReset}>
              Khôi phục
            </Button>
            <Button
              type="primary"
              className="h-[40px] flex-1"
              onClick={handleApplyFilters}
            >
              Áp dụng
            </Button>
          </div>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Tên thuốc</Text>
          <Input
            placeholder="Nhập tên thuốc..."
            value={filters.medicine}
            onChange={(e) =>
              setFilters({ ...filters, medicine: e.target.value })
            }
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select
            placeholder="Chọn trạng thái"
            allowClear
            value={filters.status || undefined}
            onChange={(value) =>
              setFilters({ ...filters, status: value || "" })
            }
            options={[
              { label: "Chờ duyệt", value: "PENDING" },
              { label: "Đã duyệt", value: "APPROVED" },
              { label: "Từ chối", value: "REJECTED" },
            ]}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Ngày yêu cầu</Text>
          <DatePicker
            className="w-full"
            placeholder="Chọn ngày"
            value={filters.date ? dayjs(filters.date) : null}
            onChange={(_, dateString) =>
              setFilters({ ...filters, date: typeof dateString === 'string' ? dateString : '' })
            }
          />
        </div>
      </BaseFilterCard>

      <BaseTable<RequestRow>
        rowKey="key"
        loading={isLoading || isInventoryLoading}
        columns={columns}
        dataSource={rows}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: (page, pageSize) => {
            setPagination((prev) => ({
              ...prev,
              current: page,
              pageSize,
            }));
          },
        }}
        cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
        title={() => (
          <div className="flex items-center justify-between">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              Quản lý yêu cầu nhập thuốc
            </Text>

            <Space>
              <Input.Search
                className="w-[260px]"
                placeholder="Tìm kiếm..."
                allowClear
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />

              <Button type="primary" onClick={() => setOpenModal(true)}>
                Tạo yêu cầu
              </Button>
            </Space>
          </div>
        )}
      />

      <CreateMedicineRequestModal
        open={openModal}
        onClose={() => setOpenModal(false)}
        onSuccess={() => {
          refetch();
        }}
      />
    </MainLayout>
  );
}
