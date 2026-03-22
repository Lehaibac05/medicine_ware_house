import { Button, Input, Popconfirm, Space, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useCallback, useEffect, useMemo, useState } from "react";
import BaseTable from "../../../components/base/BaseTable";
import {
  createSupplier,
  deleteSupplier,
  getSuppliers,
  updateSupplier,
} from "../../../services/suppliers";
import type { Supplier } from "../../../services/types";
import type { SupplierFilters } from "../SupplierPage";
import SupplierFormModal, {
  type SupplierFormValues,
} from "./SupplierFormModal";

const { Text } = Typography;

type SupplierRow = {
  key: string;
  supplierId: number;
  supplierName: string;
  contactPerson: string;
  phoneNumber: string;
  email: string;
  address: string;
  taxCode: string;
  status: string;
};

type SupplierTableProps = {
  filters?: SupplierFilters;
  search?: string;
  onSearch?: (value: string) => void;
};

const normalizeText = (value?: string | null) => value?.toLowerCase().trim() ?? "";

const renderStatus = (status?: string) => {
  if (status === "ACTIVE") {
    return <Tag color="green">Hoạt động</Tag>;
  }
  if (status === "SUSPENDED") {
    return <Tag color="orange">Tạm ngưng</Tag>;
  }
  return <Tag color="red">Ngừng hoạt động</Tag>;
};

function SupplierTable({ filters, search, onSearch }: SupplierTableProps) {
  const [data, setData] = useState<Supplier[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);
  const [messageApi, contextHolder] = message.useMessage();

  const loadSuppliers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getSuppliers({
        status: filters?.status,
        keyword: search,
        page: pagination.current - 1,
        size: pagination.pageSize,
      });

      setData(res.content);

      setPagination((prev) => ({
        ...prev,
        total: res.totalElements,
      }));
    } catch {
      messageApi.error("Không tải được danh sách nhà cung cấp");
    } finally {
      setLoading(false);
    }
  }, [filters?.status, search, pagination.current, pagination.pageSize, messageApi]);

  useEffect(() => {
    void loadSuppliers();
  }, [loadSuppliers]);

  useEffect(() => {
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }));
  }, [filters, search]);

  const rowData = useMemo<SupplierRow[]>(
    () =>
      data.map((supplier) => ({
        key: supplier.supplierId.toString(),
        supplierId: supplier.supplierId,
        supplierName: supplier.supplierName,
        contactPerson: supplier.contactPerson || "--",
        phoneNumber: supplier.phoneNumber || "--",
        email: supplier.email || "--",
        address: supplier.address || "--",
        taxCode: supplier.taxCode || "--",
        status: supplier.status || "INACTIVE",
      })),
    [data],
  );
  const openCreateModal = () => {
    setEditingSupplier(null);
    setModalOpen(true);
  };

  const openEditModal = (supplierId: number) => {
    const supplier = data.find((item) => item.supplierId === supplierId);
    if (!supplier) {
      return;
    }

    setEditingSupplier(supplier);
    setModalOpen(true);
  };

  const closeModal = () => {
    setModalOpen(false);
    setEditingSupplier(null);
  };

  const handleSubmit = async (values: SupplierFormValues) => {
    setSubmitting(true);
    try {
      if (editingSupplier) {
        await updateSupplier(editingSupplier.supplierId, values);
        messageApi.success("Cập nhật nhà cung cấp thành công");
      } else {
        await createSupplier(values);
        messageApi.success("Thêm nhà cung cấp thành công");
      }

      closeModal();
      await loadSuppliers();
    } catch {
      messageApi.error("Không thể lưu thông tin nhà cung cấp");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (supplierId: number) => {
    try {
      await deleteSupplier(supplierId);
      messageApi.success("Đã chuyển nhà cung cấp sang trạng thái ngừng hoạt động");
      await loadSuppliers();
    } catch {
      messageApi.error("Không thể cập nhật trạng thái nhà cung cấp");
    }
  };

  const columns: ColumnsType<SupplierRow> = [
    {
      title: "Mã NCC",
      dataIndex: "supplierId",
      key: "supplierId",
      width: 110,
      render: (value: number) => <Text strong>{value}</Text>,
    },
    {
      title: "Tên nhà cung cấp",
      dataIndex: "supplierName",
      key: "supplierName",
      width: 240,
      render: (value: string) => <Text strong>{value}</Text>,
    },
    {
      title: "Người liên hệ",
      dataIndex: "contactPerson",
      key: "contactPerson",
      width: 180,
    },
    {
      title: "Điện thoại",
      dataIndex: "phoneNumber",
      key: "phoneNumber",
      width: 140,
    },
    {
      title: "Email",
      dataIndex: "email",
      key: "email",
      width: 220,
    },
    {
      title: "Mã số thuế",
      dataIndex: "taxCode",
      key: "taxCode",
      width: 160,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      width: 140,
      render: (value: string) => renderStatus(value),
    },
    {
      title: "Địa chỉ",
      dataIndex: "address",
      key: "address",
      width: 260,
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
            onClick={() => openEditModal(record.supplierId)}
          >
            Sửa
          </Button>
          <Popconfirm
            title="Ngừng nhà cung cấp"
            description="Thao tác này sẽ chuyển trạng thái sang ngừng hoạt động."
            onConfirm={() => handleDelete(record.supplierId)}
          >
            <Button size="small" danger>
              Ngừng
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
              Danh sách nhà cung cấp
            </Text>

            <Button type="primary" onClick={openCreateModal}>
              Thêm nhà cung cấp
            </Button>
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
            }));
          },
        }}
      />

      <SupplierFormModal
        open={modalOpen}
        mode={editingSupplier ? "edit" : "create"}
        loading={submitting}
        initialValues={
          editingSupplier
            ? {
              supplierName: editingSupplier.supplierName,
              contactPerson: editingSupplier.contactPerson || "",
              phoneNumber: editingSupplier.phoneNumber || "",
              email: editingSupplier.email || "",
              address: editingSupplier.address || "",
              taxCode: editingSupplier.taxCode || "",
              status: editingSupplier.status || "ACTIVE",
            }
            : undefined
        }
        onCancel={closeModal}
        onSubmit={handleSubmit}
      />
    </>
  );
}

export default SupplierTable;
