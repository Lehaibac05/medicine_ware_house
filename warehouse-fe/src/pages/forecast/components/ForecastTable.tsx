import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";
import { Flex, Typography, Tag, Button, message } from "antd";
import { useCallback, useEffect, useState } from "react";
import { forecastApi } from "../../../services/forecast";
import { getAllMedicines } from "../../../services/medicines";
import { getInventory } from "../../../services/inventory";

const { Text } = Typography;

interface ForecastTableProps {
  onDataUpdate?: (data: any[]) => void;
}

type ForecastRow = {
  key: string;
  medicine_name: string;
  forecast_period: string;
  predicted_quantity: string;
  confidence_level: string;
  risk_level: string;
  suggested_action: string;
  recommended_order: string;
};

const columns: ColumnsType<ForecastRow> = [
  {
    title: "Thuốc",
    dataIndex: "medicine_name",
    key: "medicine_name",
  },
  {
    title: "Kỳ dự báo",
    dataIndex: "forecast_period",
    key: "forecast_period",
  },
  {
    title: "Dự tính tiêu thụ",
    dataIndex: "predicted_quantity",
    key: "predicted_quantity",
  },
  {
    title: "Mức độ tin cậy",
    dataIndex: "confidence_level",
    key: "confidence_level",
    render: (value: string) => {
      const confidence = Number(value);
      if (!Number.isFinite(confidence)) {
        return <Tag color="default">-</Tag>;
      }
      let color = "red";
      if (confidence >= 0.8) color = "green";
      else if (confidence >= 0.6) color = "orange";
      const percentage = (confidence * 100).toFixed(2);
      return <Tag color={color}>{percentage}%</Tag>;
    },
  },
  {
    title: "Mức độ rủi ro",
    dataIndex: "risk_level",
    key: "risk_level",
    render: (value: string) => {
      let color = "green";
      if (value === "HIGH") color = "red";
      else if (value === "MEDIUM") color = "orange";
      return <Tag color={color}>{value}</Tag>;
    },
  },
  {
    title: "Hành động đề xuất",
    dataIndex: "suggested_action",
    key: "suggested_action",
  },
  {
    title: "Đề xuất đặt hàng",
    dataIndex: "recommended_order",
    key: "recommended_order",
    render: (value: string) => <Tag color="blue">{value || "-"}</Tag>,
  },
];

function ForecastTable({ onDataUpdate }: ForecastTableProps) {
  const [forecasts, setForecasts] = useState<ForecastRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [messageApi, contextHolder] = message.useMessage();

  const loadData = useCallback(async () => {
    try {
      setLoading(true);

      // Lấy danh sách tất cả thuốc
      const medicinesData = await getAllMedicines();
      const medicineMapData = Object.fromEntries(
        medicinesData.map((m) => [m.medicineId, m.name])
      );

      // Lấy dữ liệu dự báo cho từng thuốc (song song nhưng giới hạn số lượng)
      const allForecastData: ForecastRow[] = [];
      const medicinesToProcess = medicinesData.slice(0, 10); // Giới hạn 10 thuốc

      const inventoryData = await getInventory({ page: 0, size: 1000 });
      const inventoryRows = Array.isArray(inventoryData) ? inventoryData : inventoryData.content;
      const inventoryByMedicine = inventoryRows.reduce<Record<number, number>>((acc, row) => {
        acc[row.medicineId] = (acc[row.medicineId] || 0) + row.totalStock;
        return acc;
      }, {});

      const forecastPromises = medicinesToProcess.map(async (medicine) => {
        try {
          const currentStock = inventoryByMedicine[medicine.medicineId] ?? 0;
          const forecastResponse = await forecastApi.predictDemand({
            medicineId: medicine.medicineId,
            medicineName: medicine.name,
            region: 'Bắc',
            currentInventory: currentStock,
            salesLag1: 50,
            salesLag7: 45,
            salesLag30: 40,
          });

          const forecastValue = Number(forecastResponse.predictedQuantity || 0);
          const rawConfidence = forecastResponse.confidenceLevel ?? forecastResponse.confidence;
          const confidenceLevel = Number.isFinite(Number(rawConfidence)) ? Number(rawConfidence) : NaN;
          const recommendedOrder = forecastResponse.recommendedOrder ?? "-";
          const riskLevel = forecastValue < 50 ? "HIGH" : forecastValue < 100 ? "MEDIUM" : "LOW";
          const hasRecommendation = recommendedOrder !== "-" && Number(recommendedOrder) > 0;
          const actionSentence = hasRecommendation
            ? `AI đề xuất đặt thêm ${recommendedOrder} đơn vị để đảm bảo tồn kho đủ cho 30 ngày tới.`
            : riskLevel === "HIGH"
            ? "Tồn kho đang thấp và nhu cầu cao; cần đặt hàng ngay để tránh thiếu hụt."
            : riskLevel === "MEDIUM"
            ? "Tồn kho khá ổn nhưng cần theo dõi sát sao; cân nhắc đặt thêm nếu xu hướng tăng." 
            : "Tồn kho hiện ổn; tiếp tục giám sát và không cần đặt thêm ngay.";

          return {
            key: `${medicine.medicineId}_${new Date().toISOString().split('T')[0]}`,
            medicine_name: medicineMapData[medicine.medicineId],
            forecast_period: `30 ngày`,
            predicted_quantity: Math.round(forecastValue).toString(),
            confidence_level: Number.isFinite(confidenceLevel) ? confidenceLevel.toFixed(2) : "-",
            risk_level: riskLevel,
            suggested_action: actionSentence,
            recommended_order: recommendedOrder.toString(),
          };
        } catch (error) {
          console.warn(`Không thể tải dự báo cho thuốc ${medicine.name}:`, error);
        }
        return null;
      });

      const forecastResults = await Promise.all(forecastPromises);
      const validForecasts = forecastResults.filter((result): result is ForecastRow => result !== null);

      setForecasts(validForecasts);
      
      // Truyên dâ liêu lên parent component cho ExecutiveNewsSummary
      if (onDataUpdate) {
        onDataUpdate(validForecasts);
      }
    } catch (error) {
      console.error("Error loading forecast data:", error);
      messageApi.error("Lỗi khi tải dữ liệu dự báo");
    } finally {
      setLoading(false);
    }
  }, [messageApi]);
  useEffect(() => {
    loadData();
  }, [loadData]);

  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Dự báo tồn kho thuốc
        </Text>
        <Text className="text-sm font-medium">
          {forecasts.length} dự báo • Cập nhật cuối: {new Date().toLocaleDateString('vi-VN')}
        </Text>
      </div>
      <Button
        type="primary"
        onClick={loadData}
        loading={loading}
      >
        Làm mới
      </Button>
    </Flex>
  );

  // Tính toán thông tin tóm tắt
  const getSummaryInfo = () => {
    const totalRecommendations = forecasts.filter(f => 
      f.recommended_order !== "-" && Number(f.recommended_order) > 0
    ).length;
    
    const highRiskItems = forecasts.filter(f => f.risk_level === "HIGH").length;
    const totalRecommendedQuantity = forecasts.reduce((sum, f) => {
      const order = Number(f.recommended_order) || 0;
      return sum + order;
    }, 0);

    return {
      totalRecommendations,
      highRiskItems,
      totalRecommendedQuantity,
      hasRecommendations: totalRecommendations > 0
    };
  };

  const summaryInfo = getSummaryInfo();

  return (
    <>
      {contextHolder}
      <BaseTable
        title={() => tableHeader}
        columns={columns}
        dataSource={forecasts}
        loading={loading}
        rowKey="key"
      />
    </>
  );
}

export default ForecastTable;
