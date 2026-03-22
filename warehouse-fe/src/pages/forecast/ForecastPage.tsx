import { Button, Select, Typography } from "antd";
import {
  LineChartOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ShoppingCartOutlined,
} from "@ant-design/icons";
import { useEffect, useState } from "react";

import BaseFilterCard from "../../components/base/BaseFilterCard";
import ForecastTable from "./components/ForecastTable";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";
import ForecastPanel from "../dashboard/components/ForecastPanel";
import MainLayout from "../../layouts/MainLayout";
import { forecastApi } from "../../services/forecast";

const { Text } = Typography;

const riskOptions = [
  { value: "all", label: "Tất cả mức độ rủi ro" },
  { value: "low", label: "Thấp" },
  { value: "medium", label: "Trung bình" },
  { value: "high", label: "Cao" },
];

const periodOptions = [
  { value: "all", label: "Tất cả kỳ" },
  { value: "q1-2026", label: "Q1 2026" },
  { value: "q2-2026", label: "Q2 2026" },
  { value: "q3-2026", label: "Q3 2026" },
  { value: "q4-2026", label: "Q4 2026" },
];

const medicineOptions = [
  { value: "all", label: "Tất cả thuốc" },
  { value: "paracetamol", label: "Paracetamol 500mg" },
  { value: "amoxicillin", label: "Amoxicillin 250mg" },
  { value: "insulin", label: "Insulin Glargine" },
  { value: "vitamin-c", label: "Vitamin C 500mg" },
];

const ForecastPage = () => {
  const [forecastStats, setForecastStats] = useState([
    {
      label: "Dự báo kỳ này",
      value: 0,
      note: "Đang tải...",
      icon: <LineChartOutlined />,
      trend: "0%",
      trendUp: true,
      color: "text-blue-600",
      bg: "bg-[#eff6ff]",
    },
    {
      label: "Rủi ro cao",
      value: 0,
      note: "Đang tải...",
      icon: <WarningOutlined />,
      trend: "0%",
      trendUp: false,
      color: "text-red-600",
      bg: "bg-[#fef2f2]",
    },
    {
      label: "Độ tin cậy TB",
      value: "0%",
      note: "Đang tải...",
      icon: <CheckCircleOutlined />,
      trend: "0%",
      trendUp: true,
      color: "text-emerald-600",
      bg: "bg-[#f0fdf4]",
    },
    {
      label: "Cần nhập thêm",
      value: 0,
      note: "Đang tải...",
      icon: <ShoppingCartOutlined />,
      trend: "0%",
      trendUp: true,
      color: "text-amber-600",
      bg: "bg-[#fffbeb]",
    },
  ]);

  const loadForecastStats = async () => {
    try {
      const forecasts = await forecastApi.getAllForecasts();

      if (forecasts.length > 0) {
        // Calculate stats from real data
        const totalForecasts = forecasts.length;
        const highRiskCount = forecasts.filter(f => f.predictedQuantity < 50).length;
        const avgConfidence = forecasts.reduce((sum, f) => sum + f.confidenceLevel, 0) / forecasts.length;
        const needRestockCount = forecasts.filter(f => f.predictedQuantity < 100).length;

        setForecastStats([
          {
            label: "Dự báo kỳ này",
            value: totalForecasts,
            note: "Dự báo cho các loại thuốc",
            icon: <LineChartOutlined />,
            trend: "+6%",
            trendUp: true,
            color: "text-blue-600",
            bg: "bg-[#eff6ff]",
          },
          {
            label: "Rủi ro cao",
            value: highRiskCount,
            note: "Nguy cơ thiếu hàng trong 30 ngày",
            icon: <WarningOutlined />,
            trend: highRiskCount > 0 ? "+2%" : "0%",
            trendUp: false,
            color: "text-red-600",
            bg: "bg-[#fef2f2]",
          },
          {
            label: "Độ tin cậy TB",
            value: `${(avgConfidence * 100).toFixed(0)}%`,
            note: "Dựa trên model AI",
            icon: <CheckCircleOutlined />,
            trend: "+1.5%",
            trendUp: true,
            color: "text-emerald-600",
            bg: "bg-[#f0fdf4]",
          },
          {
            label: "Cần nhập thêm",
            value: needRestockCount,
            note: "Đề xuất tăng tồn kho ngay",
            icon: <ShoppingCartOutlined />,
            trend: "+3%",
            trendUp: true,
            color: "text-amber-600",
            bg: "bg-[#fffbeb]",
          },
        ]);
      }
    } catch (error) {
      console.error("Error loading forecast stats:", error);
      // Keep default loading state
    }
  };

  useEffect(() => {
    loadForecastStats();
  }, []);

  return (
    <MainLayout>
      <BaseStatsGrid stats={forecastStats} />
      <ForecastPanel />
      <BaseFilterCard
        actions={
          <Button type="primary" className="h-[40px]">
            Áp dụng
          </Button>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Thuốc</Text>
          <Select options={medicineOptions} defaultValue="all" />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Mức độ rủi ro</Text>
          <Select options={riskOptions} defaultValue="all" />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Kỳ dự báo</Text>
          <Select options={periodOptions} defaultValue="all" />
        </div>
      </BaseFilterCard>

      <ForecastTable />
    </MainLayout>
  );
};

export default ForecastPage;
