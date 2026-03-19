import { Button, Flex, Modal, Space, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import BaseTable from "../../../components/base/BaseTable";
import { getInventory, type InventoryRow as InventoryApiRow } from "../../../services/inventory";

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

const mapStatus = (status: InventoryApiRow["status"]): InventoryRow["status"] => {
  if (status === "LOW_STOCK") return "Low";
  if (status === "EXPIRING_SOON") return "Expiring soon";
  return "In stock";
};

const formatDate = (value?: string) => {
  if (!value) return "-";
  return new Date(value).toLocaleDateString("en-GB");
};

function InventoryTable({ medicineName, warehouseId, status }: InventoryTableProps) {
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<InventoryRow[]>([]);
  const [adjustingRow, setAdjustingRow] = useState<InventoryRow | null>(null);

  const goToCreateRequest = () => {
    if (!adjustingRow) return;
    const suggestedQty = adjustingRow.status === "Low" ? 20 : 10;
    navigate(
      `/requests/new?medicineId=${adjustingRow.medicineId}&warehouseId=${adjustingRow.warehouseId}&suggestedQty=${suggestedQty}`,
    );
    setAdjustingRow(null);
  };

  const goToInventoryCorrection = () => {
    if (!adjustingRow) return;
    navigate(
      `/inventory/adjustments/new?medicineId=${adjustingRow.medicineId}&warehouseId=${adjustingRow.warehouseId}`,
    );
    setAdjustingRow(null);
  };

  const columns: ColumnsType<InventoryRow> = [
    {
      title: "Medicine name",
      dataIndex: "name",
      key: "name",
      render: (value: string) => <Text strong>{value}</Text>,
    },
    { title: "Batch count", dataIndex: "batchCount", key: "batchCount" },
    { title: "Expiry date", dataIndex: "expiry", key: "expiry" },
    {
      title: "Quantity",
      dataIndex: "qty",
      key: "qty",
      render: (value: number) => value.toLocaleString(),
    },
    { title: "Warehouse", dataIndex: "warehouse", key: "warehouse" },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (value: InventoryRow["status"]) => {
        if (value === "Low") return <Tag color="red">LOW_STOCK</Tag>;
        if (value === "Expiring soon") return <Tag color="orange">EXPIRING_SOON</Tag>;
        return <Tag color="green">NORMAL</Tag>;
      },
    },
    {
      title: "Action",
      key: "action",
      render: (_: unknown, record: InventoryRow) => (
        <Space>
          <Button size="small" onClick={() => navigate(`/inventory/${record.medicineId}`)}>
            View
          </Button>
          <Button size="small" type="primary" onClick={() => setAdjustingRow(record)}>
            Adjust stock
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
        });
        setRows(
          data.map((item) => ({
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
      } catch {
        messageApi.error("Failed to load inventory data");
      } finally {
        setLoading(false);
      }
    };

    void load();
  }, [medicineName, warehouseId, status, messageApi]);

  const data = useMemo(() => rows, [rows]);

  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Inventory list
        </Text>
      </div>
      {/* <div className="w-[200px]">
        <Input.Search
          placeholder="Search by payment ID..."
          className="w-[320px]"
          allowClear
          // onSearch={onSearch}
          // onChange={(e) => !e.target.value && onSearch("")}
        />
      </div> */}
    </Flex>
  );

  return (
    <>
      {contextHolder}
      <BaseTable title={() => tableHeader} columns={columns} dataSource={data} loading={loading} />
      <Modal
        open={Boolean(adjustingRow)}
        onCancel={() => setAdjustingRow(null)}
        footer={null}
        title="Adjust Stock"
      >
        {adjustingRow ? (
          <div className="space-y-4">
            <Text>
              <strong>{adjustingRow.name}</strong> at <strong>{adjustingRow.warehouse}</strong>
            </Text>
            <div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
              Choose how to adjust stock for this medicine.
            </div>
            <div className="flex flex-col gap-2">
              <Button type="primary" onClick={goToCreateRequest}>
                Replenish stock (Create request)
              </Button>
              <Button onClick={goToInventoryCorrection}>
                Inventory correction (Adjust quantity)
              </Button>
            </div>
          </div>
        ) : null}
      </Modal>
    </>
  );
}

export default InventoryTable;
