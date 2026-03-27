import { Button, Flex, Modal, Space, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import BaseTable from "../../../components/base/BaseTable";
import {
  getInventory,
  type InventoryRow as InventoryApiRow,
} from "../../../services/inventory";
import InventoryDetailModal from "./InventoryDetailModal";
import InventoryAdjustmentModal from "./InventoryAdjustmentModal";

const { Text } = Typography;

type InventoryRow = {
  key: string;
  medicineId: number;
  warehouseId: number;
  name: string;
  batchCount: number;
  expiry: string;
  qty: number;
  warehouse: string;
  status: "In stock" | "Low" | "Expiring soon";
  statusCode: "NORMAL" | "LOW_STOCK" | "EXPIRING_SOON";
};

type InventoryTableProps = {
  medicineName?: string;
  warehouseId?: number;
  status?: "NORMAL" | "LOW_STOCK" | "EXPIRING_SOON";
};

const mapStatus = (
  status: InventoryApiRow["status"],
): InventoryRow["status"] => {
  if (status === "LOW_STOCK") return "Low";
  if (status === "EXPIRING_SOON") return "Expiring soon";
  return "In stock";
};

const formatDate = (value?: string) => {
  if (!value) return "-";
  return new Date(value).toLocaleDateString("en-GB");
};

function InventoryTable({
  medicineName,
  warehouseId,
  status,
}: InventoryTableProps) {
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<InventoryRow[]>([]);
  const [adjustingRow, setAdjustingRow] = useState<InventoryRow | null>(null);
  const [selectedMedicineId, setSelectedMedicineId] = useState<number | null>(
    null,
  );
  const [showAdjustmentForm, setShowAdjustmentForm] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const goToCreateRequest = () => {
    if (!adjustingRow) return;
    const suggestedQty = adjustingRow.status === "Low" ? 20 : 10;
    navigate(
      `/requests/new?medicineId=${adjustingRow.medicineId}&warehouseId=${adjustingRow.warehouseId}&suggestedQty=${suggestedQty}`,
    );
    setAdjustingRow(null);
  };

  // const goToInventoryCorrection = () => {
  //   if (!adjustingRow) return;
  //   navigate(
  //     `/inventory/adjustments/new?medicineId=${adjustingRow.medicineId}&warehouseId=${adjustingRow.warehouseId}`,
  //   );
  //   setAdjustingRow(null);
  // };

  const goToInventoryCorrection = () => {
    setShowAdjustmentForm(true);
  };

  const columns: ColumnsType<InventoryRow> = [
    {
      title: "Tên thuốc",
      dataIndex: "name",
      key: "name",
      render: (value: string) => <Text strong>{value}</Text>,
    },
    { title: "Số lượng lô", dataIndex: "batchCount", key: "batchCount" },
    { title: "Hạn sử dụng", dataIndex: "expiry", key: "expiry" },
    {
      title: "Số lượng",
      dataIndex: "qty",
      key: "qty",
      render: (value: number) => value.toLocaleString(),
    },
    { title: "Kho", dataIndex: "warehouse", key: "warehouse" },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (value: InventoryRow["status"]) => {
        if (value === "Low") return <Tag color="red">TỒN KHO THẤP</Tag>;
        if (value === "Expiring soon")
          return <Tag color="orange">SẮP HẾT HẠN</Tag>;
        return <Tag color="green">CÒN HÀNG</Tag>;
      },
    },
    {
      title: "Hành động",
      key: "action",
      render: (_: unknown, record: InventoryRow) => (
        <Space>
          <Button
            size="small"
            onClick={() => setSelectedMedicineId(record.medicineId)}
          >
            Xem
          </Button>
          <Button
            size="small"
            type="primary"
            onClick={() => setAdjustingRow(record)}
          >
            Điều chỉnh
          </Button>
        </Space>
      ),
    },
  ];

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const data = await getInventory({
          medicineName: medicineName || undefined,
          warehouseId,
          status,
          page: pagination.current - 1,
          size: pagination.pageSize,
        });

        let inventoryData: InventoryApiRow[];
        let total: number;

        if (Array.isArray(data)) {
          // Non-paginated response
          inventoryData = data;
          total = data.length;
        } else {
          // Paginated response
          inventoryData = data.content;
          total = data.totalElements;
        } // For now, since we're getting all data
        setRows(
          inventoryData.map((item: InventoryApiRow) => ({
            key: `${item.medicineId}-${item.warehouseId}`,
            medicineId: item.medicineId,
            warehouseId: item.warehouseId,
            name: item.medicineName,
            batchCount: item.batchCount,
            expiry: formatDate(item.nearestExpiryDate),
            qty: item.totalStock,
            warehouse: item.warehouseName,
            status: mapStatus(item.status),
            statusCode: item.status,
          })),
        );
        setPagination((prev) => ({
          ...prev,
          total,
        }));
      } catch {
        messageApi.error("Không thể tải dữ liệu tồn kho");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [medicineName, warehouseId, status, pagination.current, pagination.pageSize, messageApi]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }));
  }, [medicineName, warehouseId, status]);

  const data = useMemo(() => rows, [rows]);

  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Danh sách tồn kho
        </Text>
      </div>

      <Space>
        <Link to="/requests/new">
          <Button type="primary">Tạo yêu cầu nhập thuốc</Button>
        </Link>
      </Space>
    </Flex>
  );

  return (
    <>
      {contextHolder}
      <BaseTable
        title={() => tableHeader}
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: (page, pageSize) => {
            setPagination((prev) => ({
              ...prev,
              current: page,
              pageSize,
            }))
          },
        }}
      />
      <Modal
        open={Boolean(adjustingRow)}
        onCancel={() => setAdjustingRow(null)}
        footer={null}
        title="Điều chỉnh tồn kho"
      >
        {adjustingRow ? (
          <div className="space-y-4">
            <Text>
              <strong>{adjustingRow.name}</strong> at{" "}
              <strong>{adjustingRow.warehouse}</strong>
            </Text>
            <div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
              Chọn cách điều chỉnh tồn kho cho thuốc này.
            </div>
            <div className="flex flex-col gap-2">
              <Button type="primary" onClick={goToCreateRequest}>
                Nhập thêm (Tạo yêu cầu)
              </Button>
              <Button onClick={goToInventoryCorrection}>
                Điều chỉnh số lượng tồn kho
              </Button>
            </div>
          </div>
        ) : null}
      </Modal>

      <Modal
        open={selectedMedicineId !== null}
        onCancel={() => setSelectedMedicineId(null)}
        footer={null}
        width={900}
        title="Chi tiết tồn kho"
      >
        {selectedMedicineId && (
          <InventoryDetailModal medicineId={selectedMedicineId} />
        )}
      </Modal>

      <Modal
        open={showAdjustmentForm}
        onCancel={() => setShowAdjustmentForm(false)}
        footer={null}
        title="Chỉnh sửa số lượng tồn kho"
        width={700}
      >
        {adjustingRow && (
          <InventoryAdjustmentModal
            medicineId={adjustingRow.medicineId}
            warehouseId={adjustingRow.warehouseId}
            onSuccess={() => {
              setShowAdjustmentForm(false);
              setAdjustingRow(null);
              // reload data nếu cần
            }}
          />
        )}
      </Modal>
    </>
  );
}

export default InventoryTable;
