import { Button, Input, Popconfirm, Space, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useCallback, useEffect, useMemo, useState } from "react";
import BaseTable from "../../../components/base/BaseTable";
import {
  createWarehouse,
  deleteWarehouse,
  getWarehouses,
  updateWarehouse,
} from "../../../services/warehouses";
import type { Warehouse } from "../../../services/types";
import type { WarehouseFilters } from "../WarehousePage";
import WarehouseFormModal, {
  type WarehouseFormValues,
} from "./WarehouseFormModal";

const { Text } = Typography;

type WarehouseRow = {
  key: string;
  warehouseId: number;
  name: string;
  location: string;
  description: string;
};

type WarehouseTableProps = {
  filters?: WarehouseFilters;
  search?: string;
  onSearch?: (value: string) => void;
};

const normalizeText = (value?: string | null) => value?.toLowerCase().trim() ?? "";

function WarehouseTable({ filters, search, onSearch }: WarehouseTableProps) {
  const [data, setData] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingWarehouse, setEditingWarehouse] = useState<Warehouse | null>(null);
  const [messageApi, contextHolder] = message.useMessage();
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  const loadWarehouses = useCallback(async () => {
    setLoading(true);
    try {
      const response = await getWarehouses({
        page: pagination.current - 1,
        size: pagination.pageSize,
      });
      const warehouses = Array.isArray(response) ? response : response.content;
      const total = Array.isArray(response) ? response.length : response.totalElements;
      setData(warehouses);
      setPagination((prev) => ({
        ...prev,
        total,
      }));
    } catch {
      messageApi.error("Không tải được danh sách kho");
    } finally {
      setLoading(false);
    }
  }, [pagination.current, pagination.pageSize, messageApi]);

  useEffect(() => {
    void loadWarehouses();
  }, [loadWarehouses]);

  const filteredData = useMemo(() => {
    const normalizedSearch = normalizeText(search);
    const normalizedLocation = normalizeText(filters?.location);

    return data.filter((warehouse) => {
      const matchesLocation =
        !normalizedLocation ||
        normalizeText(warehouse.location).includes(normalizedLocation);

      const matchesSearch =
        !normalizedSearch ||
        normalizeText(warehouse.name).includes(normalizedSearch) ||
        normalizeText(warehouse.location).includes(normalizedSearch) ||
        normalizeText(warehouse.description).includes(normalizedSearch);

      return matchesLocation && matchesSearch;
    });
  }, [data, filters?.location, search]);

  const rowData = useMemo<WarehouseRow[]>(
    () =>
      filteredData.map((warehouse) => ({
        key: warehouse.warehouseId.toString(),
        warehouseId: warehouse.warehouseId,
        name: warehouse.name || "--",
        location: warehouse.location || "--",
        description: warehouse.description || "--",
      })),
    [filteredData],
  );

  const openCreateModal = () => {
    setEditingWarehouse(null);
    setModalOpen(true);
  };

  const openEditModal = (warehouseId: number) => {
    const warehouse = data.find((item) => item.warehouseId === warehouseId);
    if (!warehouse) {
      return;
    }

    setEditingWarehouse(warehouse);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingWarehouse(null);
  };

  const handleSubmit = async (values: WarehouseFormValues) => {
    setSubmitting(true);
    try {
      if (editingWarehouse) {
        await updateWarehouse(editingWarehouse.warehouseId, values);
        messageApi.success("Cập nhật kho thành công");
      } else {
        await createWarehouse(values);
        messageApi.success("Thêm kho thành công");
      }

      closeModal();
      await loadWarehouses();
    } catch {
      messageApi.error("Không thể lưu thông tin kho");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (warehouseId: number) => {
    try {
      await deleteWarehouse(warehouseId);
      messageApi.success("Xóa kho thành công");
      await loadWarehouses();
    } catch {
      messageApi.error("Không thể xóa kho");
    }
  };

  const columns: ColumnsType<WarehouseRow> = [
    {
      title: "Mã kho",
      dataIndex: "warehouseId",
      key: "warehouseId",
      width: 120,
      render: (value: number) => <Text strong>{value}</Text>,
    },
    {
      title: "Tên kho",
      dataIndex: "name",
      key: "name",
      width: 240,
      render: (value: string) => <Text strong>{value}</Text>,
    },
    {
      title: "Vị trí",
      dataIndex: "location",
      key: "location",
      width: 280,
    },
    {
      title: "Mô tả",
      dataIndex: "description",
      key: "description",
    },
    {
      title: "Hành động",
      key: "actions",
      width: 160,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            type="primary"
            onClick={() => openEditModal(record.warehouseId)}
          >
            Sửa
          </Button>
          <Popconfirm
            title="Xóa kho"
            description="Bạn có chắc muốn xóa kho này?"
            onConfirm={() => handleDelete(record.warehouseId)}
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
          <div className="flex items-center justify-between">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              Danh sách kho
            </Text>

            <div className="flex items-center gap-3">
              <Input.Search
                className="w-[300px]"
                placeholder="Tìm theo tên kho hoặc vị trí..."
                value={search}
                onChange={(event) => onSearch?.(event.target.value)}
                allowClear
              />
              <Button type="primary" onClick={openCreateModal}>
                Thêm kho
              </Button>
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

      <WarehouseFormModal
        open={modalOpen}
        mode={editingWarehouse ? "edit" : "create"}
        loading={submitting}
        initialValues={
          editingWarehouse
            ? {
                name: editingWarehouse.name || "",
                location: editingWarehouse.location || "",
                description: editingWarehouse.description || "",
              }
            : undefined
        }
        onCancel={closeModal}
        onSubmit={handleSubmit}
      />
    </>
  );
}

export default WarehouseTable;
