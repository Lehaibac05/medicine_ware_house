import { Button, Space, Tag, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useState } from "react";
import dayjs from "dayjs";
import BaseTable from "../../../components/base/BaseTable";
import { getAllBatches } from "../../../services/batches";
import type { Batch } from "../../../services/types";

type BatchRow = {
  key: string;
  batchId: number;
  batch: string;
  name: string;
  mfg: string;
  expiry: string;
  quantity: string;
  warehouse: string;
  status: "In stock" | "Near Expiry" | "Expired";
};

function BatchTable() {
  const [data, setData] = useState<BatchRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    loadBatches();
  }, []);

  const loadBatches = async () => {
    setLoading(true);
    try {
      const batches = await getAllBatches();
      const rows: BatchRow[] = batches.map((batch) => {
        // Calculate status based on expiry date
        const expiryDate = dayjs(batch.expiryDate);
        const today = dayjs();
        const daysUntilExpiry = expiryDate.diff(today, "day");
        
        let status: BatchRow["status"] = "In stock";
        if (daysUntilExpiry < 0) {
          status = "Expired";
        } else if (daysUntilExpiry <= 30) {
          status = "Near Expiry";
        }

        return {
          key: batch.batchId.toString(),
          batchId: batch.batchId,
          batch: batch.lotNumber,
          name: batch.medicine?.name || "N/A",
          mfg: dayjs(batch.manufactureDate).format("DD/MM/YYYY"),
          expiry: dayjs(batch.expiryDate).format("DD/MM/YYYY"),
          quantity: batch.quantity.toLocaleString(),
          warehouse: batch.warehouse?.name || "N/A",
          status,
        };
      });
      setData(rows);
    } catch (error) {
      messageApi.error("Failed to load batches");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<BatchRow> = [
    {
      title: "Batch number",
      dataIndex: "batch",
      key: "batch",
    },
    {
      title: "Medicine name",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Mfg date",
      dataIndex: "mfg",
      key: "mfg",
    },
    {
      title: "Expiry date",
      dataIndex: "expiry",
      key: "expiry",
    },
    {
      title: "Quantity",
      dataIndex: "quantity",
      key: "quantity",
    },
    {
      title: "Warehouse",
      dataIndex: "warehouse",
      key: "warehouse",
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (value: BatchRow["status"]) => {
        if (value === "Near Expiry") return <Tag color="gold">Near Expiry</Tag>;
        if (value === "In stock") return <Tag color="green">In Stock</Tag>;
        return <Tag color="red">Expired</Tag>;
      },
    },
    {
      title: "Action",
      key: "action",
      render: () => (
        <Space>
          <Button size="small">View</Button>
          <Button size="small" type="primary">
            Adjust Stock
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      {contextHolder}
      <BaseTable columns={columns} dataSource={data} loading={loading} />
    </>
  );
}

export default BatchTable;
