import { Button, Space, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import isBetween from "dayjs/plugin/isBetween";
import BaseTable from "../../../components/base/BaseTable";
import type { InventoryFilter } from "../InventoryPage";
import { getAllBatches } from "../../../services/batches";
import type { Batch } from "../../../services/types";

dayjs.extend(isBetween);

const { Text } = Typography;

type InventoryRow = {
  key: string;
  batchId: number;
  name: string;
  batch: string;
  expiry: string;
  qty: number;
  warehouse: string;
  status: "In stock" | "Low" | "Overstock";
};

type InventoryTableProps = {
  filter: InventoryFilter;
};

function InventoryTable({ filter }: InventoryTableProps) {
  const [batches, setBatches] = useState<Batch[]>([]);
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    loadBatches();
  }, []);

  const loadBatches = async () => {
    setLoading(true);
    try {
      const data = await getAllBatches();
      setBatches(data || []);
    } catch (error) {
      messageApi.error("Failed to load inventory");
      console.error(error);
      setBatches([]);
    } finally {
      setLoading(false);
    }
  };

  const data: InventoryRow[] = useMemo(() => {
    if (!batches || batches.length === 0) {
      return [];
    }
    
    return batches.map((batch) => {
      // Determine status based on quantity
      let status: InventoryRow["status"] = "In stock";
      if (batch.quantity < 1000) {
        status = "Low";
      } else if (batch.quantity > 5000) {
        status = "Overstock";
      }

      return {
        key: batch.batchId.toString(),
        batchId: batch.batchId,
        name: batch.medicine?.name || "N/A",
        batch: batch.lotNumber,
        expiry: dayjs(batch.expiryDate).format("DD/MM/YYYY"),
        qty: batch.quantity,
        warehouse: batch.warehouse?.name || "N/A",
        status,
      };
    });
  }, [batches]);

  const filteredData = useMemo(() => {
    return data.filter((item) => {
      // Filter by warehouse
      if (filter.warehouse !== "all") {
        const warehouseMap: Record<string, string> = {
          main: "Main Warehouse",
          "cold-1": "Cold Storage 1",
          "cold-2": "Cold Storage 2",
          secondary: "Secondary Warehouse",
        };
        if (item.warehouse !== warehouseMap[filter.warehouse]) {
          return false;
        }
      }

      // Filter by medicine name
      if (filter.medicineName) {
        if (!item.name.toLowerCase().includes(filter.medicineName.toLowerCase())) {
          return false;
        }
      }

      // Filter by status
      if (filter.status !== "all") {
        const statusMap: Record<string, string> = {
          "in-stock": "In stock",
          low: "Low",
          overstock: "Overstock",
        };
        if (item.status !== statusMap[filter.status]) {
          return false;
        }
      }

      // Filter by expiry date range
      if (filter.expiryDateRange && filter.expiryDateRange[0] && filter.expiryDateRange[1]) {
        const expiryDate = dayjs(item.expiry, "DD/MM/YYYY");
        if (
          !expiryDate.isBetween(
            filter.expiryDateRange[0],
            filter.expiryDateRange[1],
            "day",
            "[]"
          )
        ) {
          return false;
        }
      }

      return true;
    });
  }, [data, filter]);

  const columns: ColumnsType<InventoryRow> = [
    {
      title: "Medicine name",
      dataIndex: "name",
      key: "name",
      render: (value: string) => <Text strong>{value}</Text>,
    },
    { title: "Batch number", dataIndex: "batch", key: "batch" },
    { title: "Expiry date", dataIndex: "expiry", key: "expiry" },
    { 
      title: "Quantity", 
      dataIndex: "qty", 
      key: "qty",
      render: (value: number) => value.toLocaleString()
    },
    { title: "Warehouse", dataIndex: "warehouse", key: "warehouse" },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (value: InventoryRow["status"]) => {
        if (value === "Low") return <Tag color="gold">Low</Tag>;
        if (value === "Overstock") return <Tag color="green">Overstock</Tag>;
        return <Tag>In stock</Tag>;
      },
    },
    {
      title: "Action",
      key: "action",
      render: () => (
        <Space>
          <Button size="small">View</Button>
          <Button size="small" type="primary">
            Adjust stock
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      {contextHolder}
      <BaseTable columns={columns} dataSource={filteredData} loading={loading} />
    </>
  );
}

export default InventoryTable;
