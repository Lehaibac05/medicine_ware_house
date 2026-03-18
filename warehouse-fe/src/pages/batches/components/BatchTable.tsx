import {
  Button,
  Popconfirm,
  Space,
  Tag,
  Typography,
  Input,
  message,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import dayjs from "dayjs";
import BaseTable from "../../../components/base/BaseTable";
import {
  createBatch,
  deleteBatch,
  getAllBatches,
  updateBatch,
} from "../../../services/batches";
import { getAllMedicines } from "../../../services/medicines";
import type { Batch, Medicine, Warehouse } from "../../../services/types";
import { getWarehouses } from "../../../services/warehouses";
import BatchFormModal, { type BatchFormValues } from "./BatchFormModal";

const { Text } = Typography;

type BatchRow = {
  key: string;
  batchId: number;
  medicineId: number;
  batch: string;
  name: string;
  manufacturer: string;
  storageCondition: string;
  mfg: string;
  expiry: string;
  quantity: string;
  warehouse: string;
  status: "AVAILABLE" | "EXPIRED";
};

type BatchFilters = {
  medicineName: string;
  manufacturer: string;
  storageCondition: string;
  status: string;
};

type BatchTableProps = {
  filters?: BatchFilters;
  search?: string;
  onSearch?: (value: string) => void;
};

const getBatchStatus = (expiryDate: string): BatchRow["status"] => {
  const expiry = dayjs(expiryDate);
  const daysUntilExpiry = expiry.diff(dayjs(), "day");

  if (daysUntilExpiry < 0) return "EXPIRED";
  return "AVAILABLE";
};

function BatchTable({ filters, search, onSearch }: BatchTableProps) {
  const [data, setData] = useState<Batch[]>([]);
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingBatch, setEditingBatch] = useState<Batch | null>(null);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    void loadPageData();
  }, []);

  const loadPageData = async () => {
    setLoading(true);
    try {
      const [batchesResponse, medicinesResponse, warehousesResponse] =
        await Promise.all([
          getAllBatches(),
          getAllMedicines(),
          getWarehouses(),
        ]);
      setData(batchesResponse);
      setMedicines(medicinesResponse);
      setWarehouses(warehousesResponse);
    } catch (error) {
      messageApi.error("Failed to load batch data");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const loadBatches = async () => {
    setLoading(true);
    try {
      const batches = await getAllBatches();
      setData(batches);
    } catch (error) {
      messageApi.error("Failed to load batches");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const rowData = useMemo<BatchRow[]>(() => {
    const normalizedName = search?.toLowerCase().trim() || "";
    const selectedManufacturer = filters?.manufacturer || "all";
    const selectedStorage = filters?.storageCondition || "all";
    const selectedStatus = filters?.status || "all";

    return data
      .map((batch) => {
        const status = getBatchStatus(batch.expiryDate);
        const medicine = batch.medicine;
        const warehouse = batch.warehouse;

        return {
          key: batch.batchId.toString(),
          batchId: batch.batchId,
          medicineId: medicine?.medicineId ?? -1,
          batch: batch.lotNumber,
          name: medicine?.name || "N/A",
          manufacturer: medicine?.manufacturer || "",
          storageCondition: medicine?.storageCondition || "",
          mfg: dayjs(batch.manufactureDate).format("DD/MM/YYYY"),
          expiry: dayjs(batch.expiryDate).format("DD/MM/YYYY"),
          quantity: batch.quantity.toLocaleString(),
          warehouse: warehouse?.name || "N/A",
          status,
        };
      })
      .filter((row) => {
        if (
          normalizedName &&
          !row.name.toLowerCase().includes(normalizedName)
        ) {
          return false;
        }
        if (
          selectedManufacturer !== "all" &&
          row.manufacturer.toLowerCase() !== selectedManufacturer.toLowerCase()
        ) {
          return false;
        }
        if (
          selectedStorage !== "all" &&
          row.storageCondition.toLowerCase() !== selectedStorage.toLowerCase()
        ) {
          return false;
        }
        if (
          selectedStatus !== "all" &&
          row.status.toLowerCase() !== selectedStatus.toLowerCase()
        ) {
          return false;
        }
        return true;
      });
  }, [data, filters]);

  const openCreateModal = () => {
    setEditingBatch(null);
    setModalOpen(true);
  };

  const openEditModal = (batchId: number) => {
    const batch = data.find((item) => item.batchId === batchId);
    if (!batch) return;
    setEditingBatch(batch);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingBatch(null);
  };

  const handleSubmit = async (values: BatchFormValues) => {
    setSubmitting(true);
    try {
      const computedStatus = getBatchStatus(values.expiryDate.toISOString());
      const payload = {
        lotNumber: values.lotNumber.trim(),
        manufactureDate: values.manufactureDate.format("YYYY-MM-DD"),
        expiryDate: values.expiryDate.format("YYYY-MM-DD"),
        quantity: values.quantity,
        status: computedStatus,
        warehouse: {
          warehouseId: values.warehouseId,
        },
      };

      if (editingBatch) {
        const existingMedicineId = editingBatch.medicine?.medicineId;
        if (!existingMedicineId) {
          messageApi.error("This batch has invalid medicine information");
          return;
        }
        await updateBatch(existingMedicineId, editingBatch.batchId, payload);
        messageApi.success("Batch updated successfully");
      } else {
        await createBatch(values.medicineId, payload);
        messageApi.success("Batch created successfully");
      }

      closeModal();
      await loadBatches();
    } catch (error) {
      messageApi.error("Failed to save batch");
      console.error(error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (batch: BatchRow) => {
    try {
      if (batch.medicineId < 0) {
        messageApi.error("This batch has invalid medicine information");
        return;
      }
      await deleteBatch(batch.medicineId, batch.batchId);
      messageApi.success("Batch deleted successfully");
      await loadBatches();
    } catch (error) {
      messageApi.error("Failed to delete batch");
      console.error(error);
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
        if (value === "AVAILABLE") return <Tag color="green">AVAILABLE</Tag>;
        return <Tag color="red">EXPIRED</Tag>;
      },
    },
    {
      title: "Action",
      key: "action",
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            type="primary"
            onClick={() => openEditModal(record.batchId)}
          >
            Edit
          </Button>
          <Popconfirm
            title="Delete batch"
            description="Are you sure you want to delete this batch?"
            onConfirm={() => handleDelete(record)}
            okText="Delete"
            cancelText="Cancel"
          >
            <Button size="small" danger>
              Delete
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      {contextHolder}
      <BaseTable
        title={() => (
          <div className="flex justify-between items-center">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              Batches list
            </Text>

            <div className="flex items-center gap-3">
              <Input.Search
                className="w-[300px]"
                placeholder="Search medicine..."
                value={search}
                onChange={(e) => onSearch?.(e.target.value)}
                allowClear
              />

              <Button type="primary" onClick={openCreateModal}>
                Add batch
              </Button>
            </div>
          </div>
        )}
        columns={columns}
        dataSource={rowData}
        loading={loading}
      />
      <BatchFormModal
        open={modalOpen}
        mode={editingBatch ? "edit" : "create"}
        loading={submitting}
        medicines={medicines}
        warehouses={warehouses}
        initialValues={
          editingBatch
            ? {
                medicineId: editingBatch.medicine?.medicineId,
                lotNumber: editingBatch.lotNumber,
                manufactureDate: dayjs(editingBatch.manufactureDate),
                expiryDate: dayjs(editingBatch.expiryDate),
                quantity: editingBatch.quantity,
                warehouseId: editingBatch.warehouse?.warehouseId,
              }
            : undefined
        }
        onCancel={closeModal}
        onSubmit={handleSubmit}
      />
    </>
  );
}

export default BatchTable;
