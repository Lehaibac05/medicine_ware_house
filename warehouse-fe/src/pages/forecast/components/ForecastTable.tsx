import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";
import { Flex, Typography, Tag, Button, message } from "antd";
import { useCallback, useEffect, useState } from "react";
import { forecastApi } from "../../../services/forecast";
import { getAllMedicines } from "../../../services/medicines";
import type { Medicine } from "../../../services/types";

const { Text } = Typography;

type ForecastRow = {
  key: string;
  medicine_name: string;
  forecast_period: string;
  predicted_quantity: string;
  confidence_level: string;
  risk_level: string;
  suggested_action: string;
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
    title: "Số lượng dự báo",
    dataIndex: "predicted_quantity",
    key: "predicted_quantity",
  },
  {
    title: "Mức độ tin cậy",
    dataIndex: "confidence_level",
    key: "confidence_level",
    render: (value: string) => {
      const confidence = parseFloat(value);
      let color = "red";
      if (confidence >= 0.8) color = "green";
      else if (confidence >= 0.6) color = "orange";
      return <Tag color={color}>{value}</Tag>;
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
];

function ForecastTable() {
  const [forecasts, setForecasts] = useState<ForecastRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [messageApi, contextHolder] = message.useMessage();

  const generatePredictions = useCallback(async (medicinesData: Medicine[]) => {
    try {
      const predictions = [];

      for (const medicine of medicinesData.slice(0, 5)) {
        try {
          const prediction = await forecastApi.predictDemand({
            medicineId: medicine.medicineId,
            temperature: 30,
            fluSeason: Math.random() > 0.8,
            rain: Math.random() > 0.7,
          });

          predictions.push({
            key: `pred-${medicine.medicineId}`,
            medicine_name: medicine.name,
            forecast_period: prediction.period || "2026-Q1",
            predicted_quantity: prediction.predictedQuantity.toFixed(1),
            confidence_level: prediction.confidenceLevel.toFixed(2),
            risk_level:
              prediction.predictedQuantity < 50
                ? "HIGH"
                : prediction.predictedQuantity < 100
                  ? "MEDIUM"
                  : "LOW",
            suggested_action:
              prediction.predictedQuantity < 50 ? "Đặt hàng ngay" : "Theo dõi",
          });
        } catch (error) {
          console.error(`Error predicting for medicine ${medicine.name}:`, error);
        }
      }

      setForecasts(predictions);
    } catch (error) {
      console.error("Error generating predictions:", error);
      messageApi.error("Lỗi khi tạo dự báo");
    }
  }, [messageApi]);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);

      const medicinesData = await getAllMedicines();
      const medicineMapData = Object.fromEntries(
        medicinesData.map((m) => [m.medicineId, m.name])
      );

      const forecastsData = await forecastApi.getAllForecasts();

      if (forecastsData.length === 0) {
        await generatePredictions(medicinesData);
      } else {
        const tableData = forecastsData
          .filter((forecast) => medicineMapData[forecast.medicineId] !== undefined)
          .map((forecast) => ({
            key: forecast.forecastId.toString(),
            medicine_name: medicineMapData[forecast.medicineId],
            forecast_period: forecast.period,
            predicted_quantity: forecast.predictedQuantity.toString(),
            confidence_level: forecast.confidenceLevel.toString(),
            risk_level:
              forecast.predictedQuantity < 50
                ? "HIGH"
                : forecast.predictedQuantity < 100
                  ? "MEDIUM"
                  : "LOW",
            suggested_action:
              forecast.predictedQuantity < 50 ? "Đặt hàng ngay" : "Theo dõi",
          }));

        setForecasts(tableData);
      }
    } catch (error) {
      console.error("Error loading forecast data:", error);
      messageApi.error("Lỗi khi tải dữ liệu dự báo");
    } finally {
      setLoading(false);
    }
  }, [generatePredictions, messageApi]); 
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
