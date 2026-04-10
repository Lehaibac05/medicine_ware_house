import { Button, DatePicker, Select, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import dayjs from "dayjs";
import { useQuery } from "@tanstack/react-query";
import FilterPanel from "../../components/reporting/FilterPanel";
import MainLayout from "../../layouts/MainLayout";
import DataTable from "../../components/reporting/DataTable";
import { useFinancialReportQuery } from "../../hooks/useReports";
import { getActiveSuppliers } from "../../services/suppliers";
import {
  reportApi,
  type FinancialReportItem,
  type FinancialStatus,
} from "../../services/reports";

const { RangePicker } = DatePicker;
const { Text } = Typography;

const statusTag = (status: FinancialStatus) => {
  if (status === "PAID") return <Tag color="green">ĐÃ THANH TOÁN</Tag>;
  if (status === "PARTIAL") return <Tag color="gold">THANH TOÁN MỘT PHẦN</Tag>;
  return <Tag color="red">CHƯA THANH TOÁN</Tag>;
};

const financialStatusToVietnamese = (status: FinancialStatus): string => {
  if (status === "PAID") return "Đã thanh toán";
  if (status === "PARTIAL") return "Thanh toán một phần";
  return "Chưa thanh toán";
};

const money = (value: number) =>
  Number(value || 0).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

export default function FinancialReportPage() {
  const [messageApi, contextHolder] = message.useMessage();
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(
    null,
  );
  const [periodType, setPeriodType] = useState<
    "CUSTOM" | "MONTH" | "QUARTER" | "YEAR"
  >("CUSTOM");
  const [supplierId, setSupplierId] = useState<number | undefined>();
  const [status, setStatus] = useState<FinancialStatus | undefined>();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);

  const params = useMemo(
    () => ({
      page: page - 1,
      size,
      fromDate: dateRange?.[0]?.format("YYYY-MM-DD"),
      toDate: dateRange?.[1]?.format("YYYY-MM-DD"),
      supplierId,
      status,
    }),
    [page, size, dateRange, supplierId, status],
  );

  const { data, isLoading } = useFinancialReportQuery(params);
  const { data: suppliers = [] } = useQuery({
    queryKey: ["suppliers", "active"],
    queryFn: getActiveSuppliers,
  });

  const columns: ColumnsType<FinancialReportItem> = [
    { title: "Mã hóa đơn", dataIndex: "invoiceCode", width: 150 },
    { title: "Nhà cung cấp", dataIndex: "supplierName" },
    {
      title: "Tổng tiền",
      dataIndex: "totalAmount",
      width: 150,
      render: (v: number) => money(v),
    },
    {
      title: "Đã thanh toán",
      dataIndex: "paidAmount",
      width: 150,
      render: (v: number) => money(v),
    },
    {
      title: "Còn lại",
      dataIndex: "remainingAmount",
      width: 150,
      render: (v: number) => money(v),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 120,
      render: (value: FinancialStatus) => statusTag(value),
    },
  ];

  const onExport = async () => {
    try {
      const exportData = await reportApi.exportFinancialReport({
        fromDate: dateRange?.[0]?.format("YYYY-MM-DD"),
        toDate: dateRange?.[1]?.format("YYYY-MM-DD"),
        supplierId,
        status,
      });
      const headers = [
        "Mã hóa đơn",
        "Số hóa đơn",
        "Mã nhà cung cấp",
        "Nhà cung cấp",
        "Ngày hóa đơn",
        "Hạn thanh toán",
        "Tổng tiền",
        "Đã thanh toán",
        "Còn lại",
        "Trạng thái",
      ];
      const rows = exportData.items.map((item) => [
        item.invoiceId,
        item.invoiceCode,
        item.supplierId ?? "",
        item.supplierName ?? "",
        item.invoiceDate ?? "",
        item.dueDate ?? "",
        item.totalAmount,
        item.paidAmount,
        item.remainingAmount,
        financialStatusToVietnamese(item.status),
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
      link.download = `bao-cao-tai-chinh-${dayjs().format("YYYYMMDD-HHmmss")}.csv`;
      link.click();
      URL.revokeObjectURL(url);
      messageApi.success("File CSV tài chính đã sẵn sàng");
    } catch {
      messageApi.error("Xuất báo cáo tài chính thất bại");
    }
  };

  const applyPeriodPreset = (type: "CUSTOM" | "MONTH" | "QUARTER" | "YEAR") => {
    setPeriodType(type);
    const now = dayjs();
    if (type === "CUSTOM") {
      return;
    }
    if (type === "MONTH") {
      setDateRange([now.startOf("month"), now.endOf("month")]);
      setPage(1);
      return;
    }
    if (type === "QUARTER") {
      const quarterStartMonth = Math.floor(now.month() / 3) * 3;
      const quarterStart = now.month(quarterStartMonth).startOf("month");
      const quarterEnd = quarterStart.add(2, "month").endOf("month");
      setDateRange([quarterStart, quarterEnd]);
      setPage(1);
      return;
    }
    setDateRange([now.startOf("year"), now.endOf("year")]);
    setPage(1);
  };

  return (
    <MainLayout>
      {contextHolder}
      <FilterPanel
        onReset={() => {
          setDateRange(null);
          setPeriodType("CUSTOM");
          setSupplierId(undefined);
          setStatus(undefined);
          setPage(1);
        }}
        extraActions={
          <Button onClick={() => void onExport()}>Xuất báo cáo</Button>
        }
      >
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
          <Text className="text-xs text-slate-500">Khoảng thời gian</Text>
          <RangePicker
            className="w-full"
            value={dateRange}
            onChange={(value) => {
              setDateRange(value as [dayjs.Dayjs, dayjs.Dayjs] | null);
              setPeriodType("CUSTOM");
              setPage(1);
            }}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Nhà cung cấp</Text>
          <Select
            allowClear
            placeholder="Tất cả nhà cung cấp"
            value={supplierId}
            options={suppliers.map((supplier) => ({
              value: supplier.supplierId,
              label: supplier.supplierName,
            }))}
            onChange={(value) => {
              setSupplierId(value);
              setPage(1);
            }}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select
            allowClear
            placeholder="Tất cả trạng thái"
            value={status}
            options={[
              { value: "PAID", label: "Đã thanh toán" },
              { value: "UNPAID", label: "Chưa thanh toán" },
            ]}
            onChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
          />
        </div>

        <div className="flex items-end">
          <Text className="text-xs text-slate-500">
            Tổng: Đã xuất hóa đơn {money(Number(data?.totalInvoiceAmount || 0))}
            , Còn lại {money(Number(data?.totalRemainingAmount || 0))}
          </Text>
        </div>
      </FilterPanel>

      <DataTable<FinancialReportItem>
        rowKey="invoiceId"
        columns={columns}
        dataSource={data?.items || []}
        loading={isLoading}
        title={() => (
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            Báo cáo tài chính
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