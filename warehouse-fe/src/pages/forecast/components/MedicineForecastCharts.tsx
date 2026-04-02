import { Card, Col, Row, Select, Spin, Typography } from "antd";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { useEffect, useMemo, useState } from "react";
import { forecastApi } from "../../../services/forecast";
import type { ForecastPoint, Forecast30DayParams, ModelInfo } from "../../../services/forecast";

const { Text } = Typography;

type MedicineOption = {
  medicineId: number;
  label: string;
};

type HorizonConfig = {
  id: string;
  label: string;
  days: number;
  lineColor: string;
};

const horizonConfigs: HorizonConfig[] = [
  { id: "horizon-1", label: "Dự báo 1 ngày", days: 1, lineColor: "#0ea5e9" },
  { id: "horizon-7", label: "Dự báo 7 ngày", days: 7, lineColor: "#14b8a6" },
  { id: "horizon-30", label: "Dự báo 30 ngày", days: 30, lineColor: "#2563eb" },
];

type HorizonWithData = HorizonConfig & {
  values: ForecastPoint[];
  avgConfidence: number;
  avgLower: number;
  avgUpper: number;
  latestPrediction: ForecastPoint | null;
  hasData: boolean;
};

type MedicineForecastChartsProps = {
  medicines: MedicineOption[];
};

const defaultForecastParams: Omit<Forecast30DayParams, "medicineId" | "medicineName"> = {
  region: "Bắc",
  temperature: 28,
  fluSeason: 0,
  rain: 0,
  salesLag1: 45,
  salesLag7: 48,
  salesLag30: 45,
  storageCondition: "Room temperature",
};

function MedicineForecastCharts({ medicines }: MedicineForecastChartsProps) {
  const [selectedMedicineId, setSelectedMedicineId] = useState<number | null>(
    medicines[0]?.medicineId ?? null
  );
  const [horizonSeries, setHorizonSeries] = useState<Record<string, ForecastPoint[]>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modelInfo, setModelInfo] = useState<ModelInfo | null>(null);
  const [usingFallbackData, setUsingFallbackData] = useState(false);

  useEffect(() => {
    if (medicines.length === 0) {
      setSelectedMedicineId(null);
      return;
    }

    setSelectedMedicineId((prev) => {
      if (prev && medicines.some((medicine) => medicine.medicineId === prev)) {
        return prev;
      }
      return medicines[0].medicineId;
    });
  }, [medicines]);

  useEffect(() => {
    if (!selectedMedicineId) {
      setHorizonSeries({});
      return;
    }

    let cancelled = false;

    const medicine =
      medicines.find((m) => m.medicineId === selectedMedicineId) ?? medicines[0];

    if (!medicine) {
      setHorizonSeries({});
      setError("Chưa có loại thuốc để lấy dữ liệu dự báo.");
      setUsingFallbackData(true);
      return;
    }

    const loadForecasts = async () => {
      setLoading(true);
      setError(null);
      setUsingFallbackData(true);
      try {
        const promises = horizonConfigs.map(async (config) => {
          const params: Forecast30DayParams = {
            ...defaultForecastParams,
            medicineId: medicine.medicineId,
            medicineName: medicine.label,
            days: config.days,
          };
          const data = await forecastApi.get30DayForecast(params);
          return { id: config.id, data };
        });

        const results = await Promise.all(promises);
        if (cancelled) return;

        const series: Record<string, ForecastPoint[]> = {};
        results.forEach((result) => {
          series[result.id] = result.data;
        });
        setHorizonSeries(series);
        const hasRealData = results.some((result) =>
          result.data.some((point) => !point.isFallback)
        );
        setUsingFallbackData(!hasRealData);
      } catch (loadError) {
        console.error("Error loading medicine forecast:", loadError);
        if (!cancelled) {
          setError("Không thể tải dữ liệu dự báo AI trong lúc này.");
          setHorizonSeries({});
          setUsingFallbackData(true);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadForecasts();

    return () => {
      cancelled = true;
    };
  }, [selectedMedicineId, medicines]);

  useEffect(() => {
    const fetchModelInfo = async () => {
      try {
        const info = await forecastApi.getModelInfo();
        setModelInfo(info);
      } catch (infoError) {
        console.warn("Unable to load AI model info:", infoError);
      }
    };

    fetchModelInfo();
  }, []);

  const selectedMedicine = useMemo(() => {
    return (
      medicines.find((medicine) => medicine.medicineId === selectedMedicineId) ??
      medicines[0] ??
      null
    );
  }, [medicines, selectedMedicineId]);

  const horizonData = useMemo<HorizonWithData[]>(() => {
    return horizonConfigs.map((config) => {
      const values = horizonSeries[config.id] ?? [];
      const hasData = values.length > 0;
      const avgConfidence = hasData
        ? values.reduce((sum, point) => sum + point.confidence, 0) / values.length
        : 0;
      const avgLower = hasData
        ? values.reduce((sum, point) => sum + point.lower, 0) / values.length
        : 0;
      const avgUpper = hasData
        ? values.reduce((sum, point) => sum + point.upper, 0) / values.length
        : 0;

      return {
        ...config,
        values,
        avgConfidence,
        avgLower,
        avgUpper,
        latestPrediction: values[values.length - 1] ?? null,
        hasData,
      };
    });
  }, [horizonSeries]);

  if (!medicines.length) {
    return (
      <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
        <div className="flex h-[160px] items-center justify-center text-sm text-slate-500">
          Cần ít nhất một loại thuốc để hiển thị biểu đồ dự báo AI.
        </div>
      </Card>
    );
  }

  return (
    <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            Dự báo chiều sâu
          </Text>
          <Text className="text-lg font-semibold">
            {selectedMedicine ? selectedMedicine.label : "Chưa có thuốc"}
          </Text>
        </div>

        <div className="flex flex-col gap-1">
          <Text className="text-[11px] text-slate-500">Thuốc</Text>
          <Select<number>
            size="small"
            value={selectedMedicineId ?? undefined}
            style={{ minWidth: 180 }}
            options={medicines.map((medicine) => ({
              label: medicine.label,
              value: medicine.medicineId,
            }))}
            onChange={(value) => setSelectedMedicineId(value)}
          />
        </div>
      </div>

      {error && (
        <div className="mb-4 rounded-2xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {(usingFallbackData || modelInfo) && (
        <div className="mb-4 space-y-2">
          {usingFallbackData && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-800">
              Dữ liệu 1/7/30 ngày hiện tại là fallback vì mô hình AI chưa xử lý xong hoặc endpoint chưa trả dữ liệu thật.
            </div>
          )}
          {modelInfo && (
            <Text className="text-[11px] text-slate-500">
              {`Model ${modelInfo.modelName} v${modelInfo.version} • Độ chính xác ${(modelInfo.accuracy * 100).toFixed(0)}% • Cập nhật ${new Date(modelInfo.lastUpdated).toLocaleDateString("vi-VN")}`}
            </Text>
          )}
        </div>
      )}

      {loading ? (
        <div className="flex h-[220px] items-center justify-center">
          <Spin tip="Đang tải dữ liệu dự báo..." size="large" />
        </div>
      ) : (
        <Row gutter={[16, 16]}>
          {horizonData.map((horizon) => (
            <Col key={horizon.id} xs={24} md={12} lg={8}>
              <div className="flex h-full flex-col justify-between rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                <div>
                  <div className="flex items-center justify-between">
                    <Text className="text-xs text-slate-500">{horizon.label}</Text>
                    <Text className="text-[11px] text-slate-400">
                      {horizon.values.length
                        ? `${horizon.values.length} ngày`
                        : "Chưa có dữ liệu"}
                    </Text>
                  </div>

                  <div className="mt-3 flex items-end gap-2">
                    <Text className="text-3xl font-semibold text-slate-900">
                      {horizon.latestPrediction
                        ? Math.round(horizon.latestPrediction.predicted)
                        : "—"}
                    </Text>
                    <Text className="text-xs text-slate-400">đơn vị</Text>
                  </div>
                  <Text className="text-xs text-slate-500">
                    Độ tin cậy {Math.round(horizon.avgConfidence * 100)}%
                  </Text>
                </div>

                <div className="mt-3 h-[140px]">
                  {horizon.hasData ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart
                        data={horizon.values}
                        margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                        <XAxis
                          dataKey="date"
                          stroke="#94a3b8"
                          fontSize={10}
                          hide={horizon.values.length <= 1}
                        />
                        <YAxis
                          stroke="#94a3b8"
                          fontSize={10}
                          tickFormatter={(value) => Math.round(value)}
                        />
                        <Tooltip
                          contentStyle={{
                            borderRadius: 8,
                            border: "1px solid #e2e8f0",
                            backgroundColor: "#fff",
                          }}
                          formatter={(value) => [`${Math.round(value as number)}`, "Đơn vị"]}
                        />
                        <Line
                          type="monotone"
                          dataKey="predicted"
                          stroke={horizon.lineColor}
                          strokeWidth={2.5}
                          dot={{ r: 3, stroke: "#fff", strokeWidth: 2 }}
                          activeDot={{ r: 5 }}
                          name="Dự báo"
                        />
                        <Line
                          type="monotone"
                          dataKey="upper"
                          stroke="#fbbf24"
                          strokeWidth={1}
                          strokeDasharray="4 4"
                          dot={false}
                          name="Giới hạn trên"
                        />
                        <Line
                          type="monotone"
                          dataKey="lower"
                          stroke="#f87171"
                          strokeWidth={1}
                          strokeDasharray="4 4"
                          dot={false}
                          name="Giới hạn dưới"
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="flex h-full items-center justify-center text-[11px] text-slate-400">
                      Chưa có dữ liệu AI
                    </div>
                  )}
                </div>

                {horizon.hasData && (
                  <div className="mt-4 flex flex-wrap gap-4 text-[11px] text-slate-400">
                    <Text>Giới hạn dưới {Math.round(horizon.avgLower)}</Text>
                    <Text>Giới hạn trên {Math.round(horizon.avgUpper)}</Text>
                  </div>
                )}
              </div>
            </Col>
          ))}
        </Row>
      )}

      <Text className="mt-4 block text-[11px] text-slate-400">
        Dữ liệu lấy từ mô hình AI đã huấn luyện, làm mới khi chọn thuốc.
      </Text>
    </Card>
  );
}

export default MedicineForecastCharts;
