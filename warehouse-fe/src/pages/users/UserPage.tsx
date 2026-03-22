import { Button, DatePicker, Input, Select, Typography } from "antd";
import { useState } from "react";
import type { Dayjs } from "dayjs";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import UserTable from "./components/UserTable";
import MainLayout from "../../layouts/MainLayout";

const { Text } = Typography;
const { RangePicker } = DatePicker;

const roleOptions = [
  { value: "all", label: "Tất cả vai trò" },
  { value: "admin", label: "Quản trị viên" },
  { value: "staff", label: "Nhân viên" },
  { value: "manager", label: "Quản lý" },
  { value: "supplier", label: "Nhà cung cấp" },
  { value: "accountant", label: "Kế toán" },
];

const statusOptions = [
  { value: "all", label: "Tất cả trạng thái" },
  { value: "active", label: "Hoạt động" },
  { value: "inactive", label: "Không hoạt động" },
];

export type UserFilters = {
  role: string;
  status: string;
  userId: string;
  dateRange: [string, string] | null;
};

const UserPage = () => {
  const [role, setRole] = useState("all");
  const [status, setStatus] = useState("all");
  const [userId, setUserId] = useState("");
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);
  const [search, setSearch] = useState("");
  const [appliedFilters, setAppliedFilters] = useState<UserFilters>({
    role: "all",
    status: "all",
    userId: "",
    dateRange: null,
  });

  const handleApplyFilters = () => {
    setAppliedFilters({
      role,
      status,
      userId: userId.trim(),
      dateRange,
    });
  };

  const handleDateChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
    if (!dates?.[0] || !dates?.[1]) {
      setDateRange(null);
      return;
    }

    setDateRange([
      dates[0].format("YYYY-MM-DD"),
      dates[1].format("YYYY-MM-DD"),
    ]);
  };

  return (
    <MainLayout>
      <BaseFilterCard
        actions={
          <Button
            type="primary"
            className="h-[40px]"
            onClick={handleApplyFilters}
          >
            Áp dụng
          </Button>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Vai trò</Text>
          <Select options={roleOptions} value={role} onChange={setRole} />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select options={statusOptions} value={status} onChange={setStatus} />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Khoảng ngày đăng nhập</Text>
          <RangePicker
            className="w-full"
            format="YYYY-MM-DD"
            placeholder={["Start date", "End date"]}
            onChange={handleDateChange}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Mã người dùng</Text>
          <Input
            placeholder="Nhập mã người dùng..."
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
          />
        </div>
      </BaseFilterCard>

      <UserTable
        filters={appliedFilters}
        search={search}
        onSearch={setSearch}
      />
    </MainLayout>
  );
};

export default UserPage;
