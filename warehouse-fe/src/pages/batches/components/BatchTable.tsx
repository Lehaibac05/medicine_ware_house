import {
  Button,
  Input,
  Popconfirm,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import type { Dayjs } from "dayjs";
import { useEffect, useMemo, useState } from "react";
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
  manufactureDate: string;
  expiryDate: string;
  mfg: string;
  expiry: string;
  quantity: string;
  warehouse: string;
  warehouseId: number | null;
  status: "AVAILABLE" | "EXPIRED";
};

type BatchFilters = {
  warehouseId: number | "all";
  dateRange: [Dayjs | null, Dayjs | null] | null;
  status: string;
};

type BatchFilterOptions = {
  warehouses: Array<{
    value: number;
    label: string;
  }>;
};

type BatchTableProps = {
  filters?: BatchFilters;
  search?: string;
  onSearch?: (value: string) => void;
  onFilterOptionsChange?: (options: BatchFilterOptions) => void;
};

const getBatchStatus = (expiryDate: string): BatchRow["status"] => {
  const expiry = dayjs(expiryDate);
  const daysUntilExpiry = expiry.diff(dayjs(), "day");

  if (daysUntilExpiry < 0) return "EXPIRED";
  return "AVAILABLE";
};

function BatchTable({
  filters,
  search,
  onSearch,
  onFilterOptionsChange,
}: BatchTableProps) {
  const [data, setData] = useState<Batch[]>([]);
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingBatch, setEditingBatch] = useState<Batch | null>(null);
  const [messageApi, contextHolder] = message.useMessage();
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    void loadPageData();
  }, []);

  useEffect(() => {
    void loadBatches();
  }, [pagination.current, pagination.pageSize]);

  useEffect(() => {
    onFilterOptionsChange?.({
      warehouses: warehouses
        .map((warehouse) => ({
          value: warehouse.warehouseId,
          label: warehouse.name || `Kho ${warehouse.warehouseId}`,
        }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    });
  }, [warehouses, onFilterOptionsChange]);

  const loadPageData = async () => {
    setLoading(true);
    try {
      const batchesResponse = await getAllBatches({
        page: pagination.current - 1,
        size: pagination.pageSize,
      });
      const batchesData = Array.isArray(batchesResponse)
        ? batchesResponse
        : batchesResponse.content;
      const total =
        Array.isArray(batchesResponse) ? batchesResponse.length : batchesResponse.totalElements;
      const [medicinesResponse, warehousesResponse] = await Promise.all([
        getAllMedicines(),
        getWarehouses(),
      ]);
      setData(batchesData);
      setMedicines(medicinesResponse);
      setWarehouses(warehousesResponse);
      setPagination((prev) => ({
        ...prev,
        total,
      }));
    } catch (error) {
      messageApi.error("Lỗi khi tải dữ liệu");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const loadBatches = async () => {
    setLoading(true);
    try {
      const batchesResponse = await getAllBatches({
        page: pagination.current - 1,
        size: pagination.pageSize,
      });
      const batchesData = Array.isArray(batchesResponse)
        ? batchesResponse
        : batchesResponse.content;
      const total =
        Array.isArray(batchesResponse) ? batchesResponse.length : batchesResponse.totalElements;
      setData(batchesData);
      setPagination((prev) => ({
        ...prev,
        total,
      }));
    } catch (error) {
      messageApi.error("Lỗi khi tải dữ liệu lô thuốc.");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const rowData = useMemo<BatchRow[]>(() => {
    const normalizedName = search?.toLowerCase().trim() || "";
    const selectedWarehouseId = filters?.warehouseId || "all";
    const selectedDateRange = filters?.dateRange;
    const selectedStatus = filters?.status || "all";
    const startDate = selectedDateRange?.[0]?.startOf("day") || null;
    const endDate = selectedDateRange?.[1]?.endOf("day") || null;

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
          manufactureDate: batch.manufactureDate,
          expiryDate: batch.expiryDate,
          mfg: dayjs(batch.manufactureDate).format("DD/MM/YYYY"),
          expiry: dayjs(batch.expiryDate).format("DD/MM/YYYY"),
          quantity: batch.quantity.toLocaleString(),
          warehouse: warehouse?.name || "N/A",
          warehouseId: warehouse?.warehouseId ?? null,
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
          selectedWarehouseId !== "all" &&
          row.warehouseId !== selectedWarehouseId
        ) {
          return false;
        }
        if (startDate && dayjs(row.manufactureDate).isBefore(startDate)) {
          return false;
        }
        if (endDate && dayjs(row.expiryDate).isAfter(endDate)) {
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
  }, [data, filters, search]);

  // const openCreateModal = () => {
  //   setEditingBatch(null);
  //   setModalOpen(true);
  // };

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
          messageApi.error("Lô thuốc không có thông tin thuốc.");
          return;
        }
        await updateBatch(existingMedicineId, editingBatch.batchId, payload);
        messageApi.success("Cập nhật lô thuốc thành công.");
      } else {
        await createBatch(values.medicineId, payload);
        messageApi.success("Tạo lô thuốc thành công.");
      }

      closeModal();
      await loadBatches();
    } catch (error) {
      messageApi.error("Lỗi khi cập nhật lô thuốc.");
      console.error(error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (batch: BatchRow) => {
    try {
      if (batch.medicineId < 0) {
        messageApi.error("Lô thuốc không có thông tin thuốc.");
        return;
      }
      await deleteBatch(batch.medicineId, batch.batchId);
      messageApi.success("Xóa lô thuốc thành công.");
      await loadBatches();
    } catch (error) {
      messageApi.error("Lỗi khi xóa lô thuốc.");
      console.error(error);
    }
  };

  const columns: ColumnsType<BatchRow> = [
    {
      title: "Số lô thuốc",
      dataIndex: "batch",
      key: "batch",
    },
    {
      title: "Tên thuốc",
      dataIndex: "name",
      key: "name",
    },
    {
      title: "Ngày sản xuất",
      dataIndex: "mfg",
      key: "mfg",
    },
    {
      title: "Hạn sử dụng",
      dataIndex: "expiry",
      key: "expiry",
    },
    {
      title: "Số lượng",
      dataIndex: "quantity",
      key: "quantity",
    },
    {
      title: "Kho",
      dataIndex: "warehouse",
      key: "warehouse",
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (value: BatchRow["status"]) => {
        if (value === "AVAILABLE") return <Tag color="green">CÒN HÀNG</Tag>;
        return <Tag color="red">HẾT HẠN</Tag>;
      },
    },
    {
      title: "Hành động",
      key: "action",
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            type="primary"
            onClick={() => openEditModal(record.batchId)}
          >
            Sửa
          </Button>
          <Popconfirm
            title="Xóa lô thuốc"
            description="Bạn có chắc chắn muốn xóa lô thuốc này?"
            onConfirm={() => handleDelete(record)}
            okText="Xóa"
            cancelText="Hủy"
          >
            <Button size="small" danger>
              Xóa
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
              Danh sách lô thuốc
            </Text>

            <div className="flex items-center gap-3">
              <Input.Search
                className="w-[300px]"
                placeholder="Tìm kiếm lô thuốc..."
                value={search}
                onChange={(e) => onSearch?.(e.target.value)}
                allowClear
              />

              {/* <Button type="primary" onClick={openCreateModal}>
                Thêm lô thuốc
              </Button> */}
            </div>
          </div>
        )}
        columns={columns}
        dataSource={rowData}
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
