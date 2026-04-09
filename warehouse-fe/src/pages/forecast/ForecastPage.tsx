import { Button, Select, Typography, message } from "antd";
import {
  LineChartOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ShoppingCartOutlined,
  RobotOutlined,
} from "@ant-design/icons";
import { useEffect, useState } from "react";

import BaseFilterCard from "../../components/base/BaseFilterCard";
import ForecastTable from "./components/ForecastTable";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";
import ForecastPanel from "../dashboard/components/ForecastPanel";
import MainLayout from "../../layouts/MainLayout";
import { forecastApi } from "../../services/forecast";
import { getAllMedicines } from "../../services/medicines";

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

  const [refreshing, setRefreshing] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [refreshKey, setRefreshKey] = useState(0);

  const loadForecastStats = async () => {
    try {
      const stats = await forecastApi.getForecastStats();

      setForecastStats([
        {
          label: "Dự báo kỳ này",
          value: stats.totalForecasts,
          note: "Dự báo cho các loại thuốc",
          icon: <LineChartOutlined />,
          trend: "+6%",
          trendUp: true,
          color: "text-blue-600",
          bg: "bg-[#eff6ff]",
        },
        {
          label: "Rủi ro cao",
          value: stats.highRiskCount,
          note: "Nguy cơ thiếu hàng trong 30 ngày",
          icon: <WarningOutlined />,
          trend: stats.highRiskCount > 0 ? "+2%" : "0%",
          trendUp: false,
          color: "text-red-600",
          bg: "bg-[#fef2f2]",
        },
        {
          label: "Độ tin cậy TB",
          value: `${(stats.avgConfidence * 100).toFixed(0)}%`,
          note: "Dựa trên model AI",
          icon: <CheckCircleOutlined />,
          trend: "+1.5%",
          trendUp: true,
          color: "text-emerald-600",
          bg: "bg-[#f0fdf4]",
        },
        {
          label: "Cần nhập thêm",
          value: stats.needRestockCount,
          note: "Đề xuất tăng tồn kho ngay",
          icon: <ShoppingCartOutlined />,
          trend: "+3%",
          trendUp: true,
          color: "text-amber-600",
          bg: "bg-[#fffbeb]",
        },
      ]);
    } catch (error) {
      console.error("Error loading forecast stats:", error);
      // Keep default loading state
    }
  };

  const refreshAIPredictions = async () => {
    try {
      setRefreshing(true);
      messageApi.info("Đang cập nhật dự đoán AI...");

      const medicines = await getAllMedicines();
      const predictions = [];

      for (const medicine of medicines.slice(0, 10)) { // Limit to first 10 medicines
        try {
          const prediction = await forecastApi.predictDemand({
            medicineId: medicine.medicineId,
            medicineName: medicine.name,
            temperature: 28,
            fluSeason: false,
            rain: false,
            currentInventory: 50,
            salesLag1: 45,
            salesLag7: 48,
            salesLag30: 45,
            storageCondition: "Room temperature",
          });

          predictions.push({
            medicineId: medicine.medicineId,
            predictedQuantity: prediction.predictedQuantity,
            period: "2026-Q1",
            confidenceLevel: prediction.confidenceLevel,
            model: {
              modelId: 1,
              modelName: "DemandPredictor",
              version: "1.0",
              accuracy: prediction.confidenceLevel,
            },
          });
        } catch (error) {
          console.error(`Error predicting for ${medicine.name}:`, error);
        }
      }

      // Create forecasts in database
      for (const pred of predictions) {
        try {
          await forecastApi.createForecast(pred);
        } catch (error) {
          console.error("Error creating forecast:", error);
        }
      }

      messageApi.success(`Đã cập nhật dự đoán cho ${predictions.length} loại thuốc`);
      loadForecastStats(); // Reload stats
      setRefreshKey(prev => prev + 1); // Trigger table reload

    } catch (error) {
      console.error("Error refreshing AI predictions:", error);
      messageApi.error("Lỗi khi cập nhật dự đoán AI");
    } finally {
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadForecastStats();
  }, []);

  return (
    <MainLayout>
      {contextHolder}
      <BaseStatsGrid stats={forecastStats} />

      {/* AI Forecast Panel with increased height */}
      <ForecastPanel />

      {/* Filter section moved up */}
      <BaseFilterCard
        actions={
          <>
            <Button
              type="primary"
              icon={<RobotOutlined />}
              loading={refreshing}
              onClick={refreshAIPredictions}
              className="mr-2"
            >
              Cập nhật AI
            </Button>
            <Button type="primary" className="h-[40px]">
              Áp dụng
            </Button>
          </>
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

      <ForecastTable key={refreshKey} />
    </MainLayout>
  );
};

export default ForecastPage;
