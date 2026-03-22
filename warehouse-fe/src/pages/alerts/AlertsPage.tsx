import { Button, DatePicker, Select, Typography } from "antd";
import { useState } from "react";
import type { Dayjs } from "dayjs";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import AlertsStatsGrid from "./components/AlertsStatsGrid";
import AlertsTable from "./components/AlertsTable";
import MainLayout from "../../layouts/MainLayout";

const { Text } = Typography;

const alertTypeOptions = [
  { value: "all", label: "Tất cả loại cảnh báo" },
  { value: "LOW_STOCK", label: "Tồn kho thấp" },
  { value: "EXPIRING_SOON", label: "Sắp hết hạn" },
  { value: "EXPIRED", label: "Đã hết hạn" },
  { value: "SYSTEM", label: "Cảnh báo hệ thống" },
];

const severityOptions = [
  { value: "all", label: "Tất cả mức độ" },
  { value: "LOW", label: "Thấp" },
  { value: "MEDIUM", label: "Trung bình" },
  { value: "HIGH", label: "Cao" },
  { value: "CRITICAL", label: "Nghiêm trọng" },
];

const sortOptions = [
  { value: "date-desc", label: "Ngày (mới nhất)" },
  { value: "date-asc", label: "Ngày (cũ nhất)" },
  { value: "severity-desc", label: "Mức độ (cao → thấp)" },
];

export type AlertFilters = {
  alertType: string;
  severity: string;
  sortBy: string;
  dateRange: [Dayjs | null, Dayjs | null] | null;
  searchText: string;
};

const AlertsPage = () => {
  const [filters, setFilters] = useState<AlertFilters>({
    alertType: "all",
    severity: "all",
    sortBy: "date-desc",
    dateRange: null,
    searchText: "",
  });

  const [tempFilters, setTempFilters] = useState<AlertFilters>(filters);

  const handleApplyFilters = () => {
    setFilters(tempFilters);
  };

  const handleSearch = (value: string) => {
    setFilters((prev) => ({ ...prev, searchText: value }));
  };

  return (
    <MainLayout>
      <AlertsStatsGrid />

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
          <Text className="text-xs text-slate-500">Loại cảnh báo</Text>
          <Select
            options={alertTypeOptions}
            value={tempFilters.alertType}
            onChange={(value) =>
              setTempFilters((prev) => ({ ...prev, alertType: value }))
            }
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Mức độ cảnh báo</Text>
          <Select
            options={severityOptions}
            value={tempFilters.severity}
            onChange={(value) =>
              setTempFilters((prev) => ({ ...prev, severity: value }))
            }
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Sắp xếp theo</Text>
          <Select
            options={sortOptions}
            value={tempFilters.sortBy}
            onChange={(value) =>
              setTempFilters((prev) => ({ ...prev, sortBy: value }))
            }
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Khoảng thời gian</Text>
          <DatePicker.RangePicker
            format="DD/MM/YYYY"
            className="w-full"
            value={tempFilters.dateRange}
            onChange={(dates) =>
              setTempFilters((prev) => ({ ...prev, dateRange: dates }))
            }
          />
        </div>
      </BaseFilterCard>

      <AlertsTable filters={filters} onSearch={handleSearch} />
    </MainLayout>
  );
};

export default AlertsPage;
