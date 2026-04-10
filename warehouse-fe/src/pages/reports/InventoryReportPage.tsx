import {
  Button,
  DatePicker,
  Input,
  Select,
  Tag,
  Typography,
  message,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import dayjs from "dayjs";
import { useInventoryReportQuery } from "../../hooks/useReports";
import FilterPanel from "../../components/reporting/FilterPanel";
import MainLayout from "../../layouts/MainLayout";
import DataTable from "../../components/reporting/DataTable";
import {
  reportApi,
  type InventoryReportItem,
  type InventoryStatus,
} from "../../services/reports";
import { getWarehouses } from "../../services/warehouses";
import { useQuery } from "@tanstack/react-query";

const { RangePicker } = DatePicker;
const { Text } = Typography;

const statusTag = (status: InventoryStatus) => {
  if (status === "LOW_STOCK") return <Tag color="red">SẮP HẾT HÀNG</Tag>;
  if (status === "EXPIRING_SOON") return <Tag color="gold">SẮP HẾT HẠN</Tag>;
  return <Tag color="green">CÒN HÀNG</Tag>;
};

const inventoryStatusToVietnamese = (status: InventoryStatus): string => {
  if (status === "LOW_STOCK") return "Sắp hết hàng";
  if (status === "EXPIRING_SOON") return "Sắp hết hạn";
  return "Còn hàng";
};

export default function InventoryReportPage() {
  const [messageApi, contextHolder] = message.useMessage();
  const [medicineName, setMedicineName] = useState("");
  const [medicineGroup, setMedicineGroup] = useState("");
  const [warehouseId, setWarehouseId] = useState<number | undefined>();
  const [status, setStatus] = useState<InventoryStatus | undefined>();
  const [expiryRange, setExpiryRange] = useState<
    [dayjs.Dayjs, dayjs.Dayjs] | null
  >(null);
  const [periodType, setPeriodType] = useState<
    "CUSTOM" | "MONTH" | "QUARTER" | "YEAR"
  >("CUSTOM");
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);

  const params = useMemo(
    () => ({
      page: page - 1,
      size,
      medicineName: medicineName.trim() || undefined,
      medicineGroup: medicineGroup.trim() || undefined,
      warehouseId,
      status,
      expiryFrom: expiryRange?.[0]?.format("YYYY-MM-DD"),
      expiryTo: expiryRange?.[1]?.format("YYYY-MM-DD"),
    }),
    [page, size, medicineName, medicineGroup, warehouseId, status, expiryRange],
  );

  const { data, isLoading } = useInventoryReportQuery(params);
  const { data: warehouses = [] } = useQuery({
    queryKey: ["warehouses"],
    queryFn: () => getWarehouses(),
  });

  const columns: ColumnsType<InventoryReportItem> = [
    { title: "Tên thuốc", dataIndex: "medicineName" },
    { title: "Kho", dataIndex: "warehouseName" },
    { title: "Tổng tồn kho", dataIndex: "totalStock", width: 130 },
    { title: "Số lô", dataIndex: "batchCount", width: 120 },
    {
      title: "Hạn gần nhất",
      dataIndex: "nearestExpiryDate",
      width: 150,
      render: (value?: string) => value || "-",
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 140,
      render: (value: InventoryStatus) => statusTag(value),
    },
  ];

  const onExport = async () => {
    try {
      const exportData = await reportApi.exportInventoryReport({
        medicineName: medicineName.trim() || undefined,
        medicineGroup: medicineGroup.trim() || undefined,
        warehouseId,
        status,
        expiryFrom: expiryRange?.[0]?.format("YYYY-MM-DD"),
        expiryTo: expiryRange?.[1]?.format("YYYY-MM-DD"),
      });

      const headers = [
        "Mã thuốc",
        "Tên thuốc",
        "Mã kho",
        "Tên kho",
        "Tổng tồn kho",
        "Số lô",
        "Hạn gần nhất",
        "Trạng thái",
        "Ngưỡng đặt hàng lại",
      ];
      const rows = exportData.items.map((item) => [
        item.medicineId,
        item.medicineName,
        item.warehouseId,
        item.warehouseName,
        item.totalStock,
        item.batchCount,
        item.nearestExpiryDate || "",
        inventoryStatusToVietnamese(item.status),
        item.reorderLevel ?? "",
      ]);
      const csv = [
        headers.join(","),
        ...rows.map((row) =>
          row
            .map((v) => `"${String(v ?? "").replaceAll('"', '""')}"`)
            .join(","),
        ),
      ].join("\n");
      const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `bao-cao-ton-kho-${dayjs().format("YYYYMMDD-HHmmss")}.csv`;
      link.click();
      URL.revokeObjectURL(url);
      messageApi.success("File CSV tồn kho đã sẵn sàng");
    } catch {
      messageApi.error("Xuất báo cáo tồn kho thất bại");
    }
  };

  const applyPeriodPreset = (type: "CUSTOM" | "MONTH" | "QUARTER" | "YEAR") => {
    setPeriodType(type);
    const now = dayjs();
    if (type === "CUSTOM") {
      return;
    }
    if (type === "MONTH") {
      setExpiryRange([now.startOf("month"), now.endOf("month")]);
      setPage(1);
      return;
    }
    if (type === "QUARTER") {
      const quarterStartMonth = Math.floor(now.month() / 3) * 3;
      const quarterStart = now.month(quarterStartMonth).startOf("month");
      const quarterEnd = quarterStart.add(2, "month").endOf("month");
      setExpiryRange([quarterStart, quarterEnd]);
      setPage(1);
      return;
    }
    setExpiryRange([now.startOf("year"), now.endOf("year")]);
    setPage(1);
  };

  return (
    <MainLayout>
      {contextHolder}
      <FilterPanel
        onReset={() => {
          setMedicineName("");
          setMedicineGroup("");
          setWarehouseId(undefined);
          setStatus(undefined);
          setExpiryRange(null);
          setPeriodType("CUSTOM");
          setPage(1);
        }}
        extraActions={
          <Button onClick={() => void onExport()}>Xuất báo cáo</Button>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Kho</Text>
          <Select
            allowClear
            placeholder="Tất cả kho"
            value={warehouseId}
            options={warehouses.map((warehouse) => ({
              value: warehouse.warehouseId,
              label: warehouse.name,
            }))}
            onChange={(value) => {
              setWarehouseId(value);
              setPage(1);
            }}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Tên thuốc</Text>
          <Input
            value={medicineName}
            placeholder="Search medicine"
            onChange={(event) => {
              setMedicineName(event.target.value);
              setPage(1);
            }}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Nhóm thuốc</Text>
          <Input
            value={medicineGroup}
            placeholder="Ví dụ: nhà sản xuất / nhóm thuốc"
            onChange={(event) => {
              setMedicineGroup(event.target.value);
              setPage(1);
            }}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select
            allowClear
            value={status}
            placeholder="Tất cả trạng thái"
            options={[
              [
                { value: "LOW_STOCK", label: "SẮP HẾT HÀNG" },
                { value: "NORMAL", label: "CÒN HÀNG" },
                { value: "EXPIRING_SOON", label: "SẮP HẾT HẠN" },
              ],
            ]}
            onChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Thời gian</Text>
          <Select
            value={periodType}
            options={[
              { value: "CUSTOM", label: "Tùy chỉnh" },
              { value: "MONTH", label: "Tháng này" },
              { value: "QUARTER", label: "Quý này" },
              { value: "YEAR", label: "Năm nay" },
            ]}
            onChange={(value) => applyPeriodPreset(value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">
            Khoảng thời gian hết hạn
          </Text>
          <RangePicker
            className="w-full"
            value={expiryRange}
            onChange={(value) => {
              setExpiryRange(value as [dayjs.Dayjs, dayjs.Dayjs] | null);
              setPeriodType("CUSTOM");
              setPage(1);
            }}
          />
        </div>
      </FilterPanel>

      <DataTable<InventoryReportItem>
        rowKey={(row) => `${row.medicineId}-${row.warehouseId}`}
        columns={columns}
        dataSource={data?.items || []}
        loading={isLoading}
        title={() => (
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            Báo cáo tồn kho
          </Text>
        )}
        pagination={{
          current: page,
          pageSize: size,
          total: data?.totalElements || 0,
          showSizeChanger: true,
          pageSizeOptions: ["10", "20", "50", "100"],
          onChange: (nextPage, nextSize) => {
            setPage(nextPage);
            setSize(nextSize);
          },
        }}
      />
    </MainLayout>
  );
}
