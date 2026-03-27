import { Button, Flex, Input, Space, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useCallback, useEffect, useMemo, useState } from "react";
import BaseTable from "../../../components/base/BaseTable";
import { createUser, getUsers, updateUser } from "../../../services/users";
import type { User } from "../../../services/types";
import type { UserFilters } from "../UserPage";
import UserFormModal, { type UserFormValues } from "./UserFormModal";
import UserImportModal from "./UserImportModal";
import { getRoles } from "../../../services/role";

const { Text } = Typography;

type UserRow = {
  key: string;
  userId: number;
  username: string;
  fullName: string;
  email: string;
  role: string;
  status: string;
  lastLogin: string;
};

type UserTableProps = {
  filters?: UserFilters;
  search?: string;
  onSearch?: (value: string) => void;
};

const normalizeRole = (roleName?: string | null) =>
  roleName?.replace(/^ROLE_/i, "").toLowerCase() ?? "";

const normalizeStatus = (status?: string | null) => status?.toLowerCase() ?? "";

const formatDateTime = (value: string | null) => {
  if (!value) {
    return "Chưa đăng nhập";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(parsed);
};

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Quản trị viên",
  WAREHOUSE_MANAGER: "Quản lý kho",
  WAREHOUSE_STAFF: "Nhân viên kho",
  ACCOUNTANT: "Kế toán",
  REQUESTER: "Người yêu cầu cấp thuốc",
};

const getRoleLabelVN = (role: string) =>
  ROLE_LABELS[role] ?? role;

function UserTable({ filters, search, onSearch }: UserTableProps) {
  const [data, setData] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [statusUpdatingUserId, setStatusUpdatingUserId] = useState<number | null>(null);
  const [messageApi, contextHolder] = message.useMessage();
  const [roleOptions, setRoleOptions] = useState<
    { value: number; label: string }[]
  >([]);

  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const roles = await getRoles();
        setRoleOptions(
          roles.map((r: any) => ({
            value: r.roleId,
            label: getRoleLabelVN(r.roleName),
          }))
        );
      } catch (err) {
        console.error("Lỗi getRoles:", err);
        messageApi.error("Không tải được danh sách vai trò");
      }
    };

    fetchRoles();
  }, [messageApi]);

  useEffect(() => {
    setCurrentPage(1);
  }, [filters, search]);

  const loadUsers = useCallback(async () => {
    setLoading(true);

    try {
      const pageResponse = await getUsers({
        page: currentPage - 1,
        size: pageSize,
        search,
        sortBy: "username",
        sortDir: "asc",
      });

      setData(pageResponse.content);
      setTotalItems(pageResponse.totalElements);

      const maxPage = Math.max(pageResponse.totalPages, 1);
      if (currentPage > maxPage) {
        setCurrentPage(maxPage);
      }
    } catch {
      messageApi.error("Không tải được danh sách người dùng");
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, search, messageApi]);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  const filteredData = useMemo(() => {
    return data.filter((user) => {
      const matchesRole =
        !filters?.role ||
        filters.role === "all" ||
        normalizeRole(user.roleName) === filters.role.toLowerCase();

      const matchesStatus =
        !filters?.status ||
        filters.status === "all" ||
        normalizeStatus(user.status) === filters.status.toLowerCase();

      const matchesUserId =
        !filters?.userId || user.userId.toString().includes(filters.userId);

      const matchesDateRange =
        !filters?.dateRange ||
        !user.lastLogin ||
        (() => {
          const loginDate = user.lastLogin.slice(0, 10);
          return (
            loginDate >= filters.dateRange[0] &&
            loginDate <= filters.dateRange[1]
          );
        })();

      return (
        matchesRole && matchesStatus && matchesUserId && matchesDateRange
      );
    });
  }, [data, filters]);

  const rowData = useMemo<UserRow[]>(
    () =>
      filteredData.map((user) => ({
        key: user.userId.toString(),
        userId: user.userId,
        username: user.username,
        fullName: user.fullName,
        email: user.email,
        // role: user.roleName ?? "--",
        role: getRoleLabelVN(user.roleName ?? "") ?? "--",
        status: user.status ?? "INACTIVE",
        lastLogin: formatDateTime(user.lastLogin),
      })),
    [filteredData]
  );

  const openCreateModal = () => {
    setCreateModalOpen(true);
  };

  const openImportModal = () => {
    setImportModalOpen(true);
  };

  const closeImportModal = () => {
    setImportModalOpen(false);
  };

  const closeModal = () => {
    setCreateModalOpen(false);
  };

  const openEditModal = (userId: number) => {
    const user = data.find((item) => item.userId === userId);
    if (!user) {
      messageApi.error("Không tìm thấy người dùng");
      return;
    }

    setEditingUser(user);
    setEditModalOpen(true);
  };

  const closeEditModal = () => {
    setEditModalOpen(false);
    setEditingUser(null);
  };

  const handleCreateUser = async (values: UserFormValues) => {
    setSubmitting(true);

    try {
      await createUser({
        username: values.username.trim(),
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        status: values.status,
        roleId: values.roleId,
      });

      if (!values.roleId) {
        messageApi.error("Vui lòng chọn vai trò");
        return;
      }
      messageApi.success("Tạo người dùng thành công");
      closeModal();
      await loadUsers();
    } catch {
      messageApi.error("Không thể tạo người dùng");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditUser = async (values: UserFormValues) => {
    if (!editingUser) {
      return;
    }

    setSubmitting(true);

    try {
      await updateUser(editingUser.userId, {
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        status: values.status,
        roleId: values.roleId,
        password: values.password?.trim() ? values.password.trim() : undefined,
      });

      messageApi.success("Cập nhật người dùng thành công");
      closeEditModal();
      await loadUsers();
    } catch {
      messageApi.error("Không thể cập nhật người dùng");
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleUserStatus = async (userId: number, currentStatus: string) => {
    const nextStatus = normalizeStatus(currentStatus) === "active" ? "INACTIVE" : "ACTIVE";

    setStatusUpdatingUserId(userId);
    try {
      await updateUser(userId, { status: nextStatus });
      messageApi.success(nextStatus === "ACTIVE" ? "Đã kích hoạt tài khoản" : "Đã vô hiệu hóa tài khoản");
      await loadUsers();
    } catch {
      messageApi.error("Không thể cập nhật trạng thái tài khoản");
    } finally {
      setStatusUpdatingUserId(null);
    }
  };

  const hasLocalFilters =
    filters?.role !== "all" ||
    filters?.status !== "all" ||
    Boolean(filters?.userId) ||
    Boolean(filters?.dateRange);

  const columns: ColumnsType<UserRow> = [
    {
      title: "Mã người dùng",
      dataIndex: "userId",
      key: "userId",
      render: (value: number) => <Text strong>{value}</Text>,
      width: 160,
    },
    {
      title: "Tên đăng nhập",
      dataIndex: "username",
      key: "username",
      width: 160,
    },
    {
      title: "Họ và tên",
      dataIndex: "fullName",
      key: "fullName",
      width: 180,
    },
    {
      title: "Email",
      dataIndex: "email",
      key: "email",
      width: 220,
    },
    {
      title: "Vai trò",
      dataIndex: "role",
      key: "role",
      width: 140,
      render: (value: string) => getRoleLabelVN(value),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      width: 140,
      render: (value: string) => {
        if (normalizeStatus(value) === "active") {
          return <Tag color="green">Hoạt động</Tag>;
        }

        return <Tag color="red">Không hoạt động</Tag>;
      },
    },
    {
      title: "Lần đăng nhập cuối",
      dataIndex: "lastLogin",
      key: "lastLogin",
      width: 180,
    },
    {
      title: "Hành động",
      key: "actions",
      width: 240,
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => openEditModal(record.userId)}>
            Sửa
          </Button>
          <Button
            size="small"
            danger={normalizeStatus(record.status) === "active"}
            loading={statusUpdatingUserId === record.userId}
            onClick={() => handleToggleUserStatus(record.userId, record.status)}
          >
            {normalizeStatus(record.status) === "active" ? "Vô hiệu hóa" : "Kích hoạt"}
          </Button>
        </Space>
      ),
    },
  ];

  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Danh sách người dùng
        </Text>
      </div>
      <div className="w-[300px]">
        <div className="flex items-center gap-3">
          <Input.Search
            placeholder="Tìm kiếm theo tên hoặc email..."
            className="w-[320px]"
            value={search}
            onChange={(event) => onSearch?.(event.target.value)}
            allowClear
          />
          <Button onClick={openImportModal}>Import user</Button>
          <Button type="primary" onClick={openCreateModal}>
            Tạo người dùng
          </Button>
        </div>
      </div>
    </Flex>
  );

  return (
    <>
      {contextHolder}

      <BaseTable
        title={() => tableHeader}
        columns={columns}
        dataSource={rowData}
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize,
          total: hasLocalFilters ? rowData.length : totalItems,
          onChange: (page, nextPageSize) => {
            setCurrentPage(page);
            if (nextPageSize && nextPageSize !== pageSize) {
              setPageSize(nextPageSize);
            }
          },
        }}
        cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
      />

      <UserFormModal
        open={createModalOpen}
        roleOptions={roleOptions}
        loading={submitting}
        mode="create"
        onCancel={closeModal}
        onSubmit={handleCreateUser}
      />

      <UserFormModal
        open={editModalOpen}
        roleOptions={roleOptions}
        loading={submitting}
        mode="edit"
        initialValues={{
          username: editingUser?.username ?? "",
          fullName: editingUser?.fullName ?? "",
          email: editingUser?.email ?? "",
          status: editingUser?.status ?? "ACTIVE",
          roleId: editingUser?.roleId ?? undefined,
        }}
        onCancel={closeEditModal}
        onSubmit={handleEditUser}
      />

      <UserImportModal
        open={importModalOpen}
        onCancel={closeImportModal}
        onImported={loadUsers}
      />
    </>
  );
}

export default UserTable;
