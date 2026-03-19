import { Button, Input, Popconfirm, Space, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useCallback, useEffect, useMemo, useState } from "react";
import BaseTable from "../../../components/base/BaseTable";
import {
  createMedicine,
  deleteMedicine,
  getMedicines,
  updateMedicine,
} from "../../../services/medicines";
import type { Medicine } from "../../../services/types";
import MedicineFormModal, {
  type MedicineFormValues,
} from "./MedicineFormModal";

const { Text } = Typography;

type MedicineRow = {
  key: string;
  medicineId: number;
  name: string;
  manufacturer: string;
  storage: string;
  description: string;
};

type MedicineFilters = {
  manufacturer: string;
  storageCondition: string;
};

type MedicineTableProps = {
  filters?: MedicineFilters;
  search?: string;
  sort?: string;
  onSearch?: (value: string) => void;
};

const mapSort = (sort?: string) => {
  if (sort === "name_desc") {
    return { sortBy: "name" as const, sortDir: "desc" as const };
  }
  if (sort === "manufacturer_asc") {
    return { sortBy: "manufacturer" as const, sortDir: "asc" as const };
  }
  if (sort === "manufacturer_desc") {
    return { sortBy: "manufacturer" as const, sortDir: "desc" as const };
  }
  return { sortBy: "name" as const, sortDir: "asc" as const };
};

function MedicineTable({
  filters,
  search,
  sort,
  onSearch,
}: MedicineTableProps) {
  const [data, setData] = useState<Medicine[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingMedicine, setEditingMedicine] = useState<Medicine | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    setCurrentPage(1);
  }, [filters, search, sort]);

  const loadMedicines = useCallback(async () => {
    setLoading(true);
    try {
      const sortParams = mapSort(sort);
      const pageResponse = await getMedicines({
        page: currentPage - 1,
        size: pageSize,
        search,
        manufacturer: filters?.manufacturer,
        storageCondition: filters?.storageCondition,
        sortBy: sortParams.sortBy,
        sortDir: sortParams.sortDir,
      });
      setData(pageResponse.content);
      setTotalItems(pageResponse.totalElements);

      const maxPage = Math.max(pageResponse.totalPages, 1);
      if (currentPage > maxPage) {
        setCurrentPage(maxPage);
      }
    } catch {
      messageApi.error("Failed to load medicines");
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, search, filters, sort, messageApi]);

  useEffect(() => {
    void loadMedicines();
  }, [loadMedicines]);

  const rowData = useMemo<MedicineRow[]>(
    () =>
      data.map((med) => ({
        key: med.medicineId.toString(),
        medicineId: med.medicineId,
        name: med.name,
        manufacturer: med.manufacturer,
        storage: med.storageCondition,
        description: med.description,
      })),
    [data],
  );

  const openCreateModal = () => {
    setEditingMedicine(null);
    setModalOpen(true);
  };

  const openEditModal = (medicineId: number) => {
    const medicine = data.find((item) => item.medicineId === medicineId);
    if (!medicine) return;

    setEditingMedicine(medicine);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingMedicine(null);
  };

  const handleSubmit = async (values: MedicineFormValues) => {
    setSubmitting(true);
    try {
      if (editingMedicine) {
        await updateMedicine(editingMedicine.medicineId, values);
        messageApi.success("Medicine updated successfully");
      } else {
        await createMedicine(values);
        messageApi.success("Medicine created successfully");
      }

      closeModal();
      await loadMedicines();
    } catch {
      messageApi.error("Failed to save medicine");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (medicineId: number) => {
    try {
      await deleteMedicine(medicineId);
      messageApi.success("Medicine deleted successfully");
      await loadMedicines();
    } catch {
      messageApi.error("Failed to delete medicine");
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
    },
    {
      title: "Storage Condition",
      dataIndex: "storage",
    },
    {
      title: "Description",
      dataIndex: "description",
    },
    {
      title: "Action",
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            type="primary"
            onClick={() => openEditModal(record.medicineId)}
          >
            Edit
          </Button>

          <Popconfirm
            title="Delete medicine"
            description="Are you sure?"
            onConfirm={() => handleDelete(record.medicineId)}
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
          <div className="flex justify-between items-center p-0">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              Medicines list
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
                Add medicine
              </Button>
            </div>
          </div>
        )}
        columns={columns}
        dataSource={rowData}
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize,
          total: totalItems,
          onChange: (page, nextPageSize) => {
            setCurrentPage(page);
            if (nextPageSize && nextPageSize !== pageSize) {
              setPageSize(nextPageSize);
            }
          },
        }}
      />

      <MedicineFormModal
        open={modalOpen}
        mode={editingMedicine ? "edit" : "create"}
        loading={submitting}
        initialValues={
          editingMedicine
            ? {
                name: editingMedicine.name,
                manufacturer: editingMedicine.manufacturer,
                storageCondition: editingMedicine.storageCondition,
                description: editingMedicine.description,
              }
            : undefined
        }
        onCancel={closeModal}
        onSubmit={handleSubmit}
      />
    </>
  );
}

export default MedicineTable;