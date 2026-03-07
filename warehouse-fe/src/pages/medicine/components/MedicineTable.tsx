import { Button, Space, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useState } from "react";
import BaseTable from "../../../components/base/BaseTable";
import { getMedicines } from "../../../services/medicines";

const { Text } = Typography;

type MedicineRow = {
  key: string;
  medicineId: number;
  name: string;
  manufacturer: string;
  storage: string;
  description: string;
};

function MedicineTable() {
  const [data, setData] = useState<MedicineRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    loadMedicines();
  }, []);

  const loadMedicines = async () => {
    setLoading(true);
    try {
      const medicines = await getMedicines();
      const rows: MedicineRow[] = medicines.map((med) => ({
        key: med.medicineId.toString(),
        medicineId: med.medicineId,
        name: med.name,
        manufacturer: med.manufacturer,
        storage: med.storageCondition,
        description: med.description,
      }));
      setData(rows);
    } catch (error) {
      messageApi.error("Failed to load medicines");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<MedicineRow> = [
    {
      title: "Medicine name",
      dataIndex: "name",
      key: "name",
      render: (value: string) => <Text strong>{value}</Text>,
    },
    {
      title: "Manufacturer",
      dataIndex: "manufacturer",
      key: "manufacturer",
    },
    {
      title: "Storage Condition",
      dataIndex: "storage",
      key: "storage",
    },
    {
      title: "Description",
      dataIndex: "description",
      key: "description",
    },
    {
      title: "Action",
      key: "action",
      render: () => (
        <Space>
          <Button size="small">View</Button>
          <Button size="small" type="primary">
            Edit
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

export default MedicineTable;
