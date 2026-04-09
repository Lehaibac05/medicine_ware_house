import { Button, Card, Col, Row, Typography, Select } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import { ComposedChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine, Area } from "recharts";
import { useEffect, useMemo, useState } from "react";
import { forecastApi, type ForecastPoint } from "../../../services/forecast";
import { getMedicines, type Medicine } from "../../../services/medicines";

const { Text, Title } = Typography;

function ForecastPanel() {
  const [chartData, setChartData] = useState<ForecastPoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dataSource, setDataSource] = useState('unknown');
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [selectedMedicineId, setSelectedMedicineId] = useState<number | undefined>(undefined);
  const [selectedRegion, setSelectedRegion] = useState<string>('Bắc');
  const [hasForecasted, setHasForecasted] = useState(false);

  const selectedMedicine = medicines.find(m => m.medicineId === selectedMedicineId);

  const handleForecast = async () => {
    if (!selectedMedicineId) return;

    try {
      setError(null);
      setLoading(true);
      const data = await forecastApi.get30DayForecast(selectedMedicineId, selectedRegion);
      const sources = Array.from(new Set(data.map(item => item.dataSource || 'unknown')));
      setDataSource(sources.join(', '));
      setChartData(data);
      setHasForecasted(true);
    } catch (error) {
      console.error('Error loading forecast:', error);
      setError('Không thể tải dữ liệu dự báo AI');
      setChartData([]);
      setHasForecasted(false);
    } finally {
      setLoading(false);
    }
  };

  const computedMetrics = useMemo(() => {
    if (error || !hasForecasted) {
      return [
        { label: "Tuần này", value: "Chưa có", color: "text-slate-500", bg: "bg-slate-100" },
        { label: "Mức rủi ro", value: "Chưa có", color: "text-slate-500", bg: "bg-slate-100" },
        { label: "Đề xuất nhập", value: "Chưa có", color: "text-slate-500", bg: "bg-slate-100" },
      ];
    }
    if (!chartData.length) {
      return [
        { label: "Tuần này", value: "0%", color: "text-slate-500", bg: "bg-slate-100" },
        { label: "Mức rủi ro", value: "Không có dữ liệu", color: "text-slate-500", bg: "bg-slate-100" },
        { label: "Đề xuất nhập", value: "0 mặt hàng", color: "text-slate-500", bg: "bg-slate-100" },
      ];
    }

    const lastPoint = chartData[chartData.length - 1];
    const threshold = 70;
    const recent = chartData.slice(-7);

    const weekStart = recent[0]
      ? (recent[0].predicted ?? recent[0].forecast ?? lastPoint.predicted ?? lastPoint.forecast ?? 0)
      : (lastPoint.predicted ?? lastPoint.forecast ?? 0);
    const weekEnd = lastPoint.predicted ?? lastPoint.forecast ?? 0;
    const trend = weekStart > 0 ? ((weekEnd - weekStart) / weekStart) * 100 : 0;

    const riskLevel = chartData.some((item) => (item.predicted || item.forecast || 0) < threshold)
      ? "Cao"
      : "Thấp";

    const toRestock = chartData.filter((item) => (item.predicted || item.forecast || 0) < threshold).length;

    return [
      {
        label: "Tuần này",
        value: `${trend >= 0 ? "+" : ""}${trend.toFixed(1)}%`,
        color: trend >= 0 ? "text-emerald-600" : "text-red-600",
        bg: trend >= 0 ? "bg-[#ecfdf5]" : "bg-[#fef2f2]",
      },
      {
        label: "Mức rủi ro",
        value: riskLevel,
        color: riskLevel === "Cao" ? "text-red-600" : "text-emerald-600",
        bg: riskLevel === "Cao" ? "bg-[#fef2f2]" : "bg-[#ecfdf5]",
      },
      {
        label: "Đề xuất nhập",
        value: `${toRestock} mục`,
        color: toRestock > 0 ? "text-amber-600" : "text-emerald-600",
        bg: toRestock > 0 ? "bg-[#fffbeb]" : "bg-[#f0fdf4]",
      },
    ];
  }, [chartData, error, hasForecasted]);

  useEffect(() => {
    const loadMedicines = async () => {
      try {
        const medicinePage = await getMedicines({
          page: 0,
          size: 100, // Load all medicines for selection
          sortBy: 'name',
          sortDir: 'asc',
        });
        setMedicines(medicinePage.content);
      } catch (error) {
        console.error('Error loading medicines:', error);
      }
    };
    loadMedicines();
  }, []);

  useEffect(() => {
    // Set default medicine when medicines are loaded
    if (medicines.length > 0 && !selectedMedicineId) {
      setSelectedMedicineId(medicines[0].medicineId);
    }
  }, [medicines, selectedMedicineId]);

  const todayDate = new Date().toLocaleDateString('en-CA');
  const showTodayLine = chartData.some(point => point.date === todayDate);

  return (
    <Card className="rounded-2xl border-0 shadow-[0_18px_32px_rgba(15,23,42,0.08)] bg-white" bodyStyle={{ padding: 28 }}>

      {/* Header */}
      <div className="mb-5 rounded-3xl border border-slate-200/70 bg-slate-50 p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="flex-1">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              AI Forecast
            </Text>

            <Title level={4} className="m-0 mt-1 font-semibold text-slate-900">
              Dự báo nhu cầu 30 ngày
            </Title>
            <Text type="secondary" className="text-xs leading-snug">
              {hasForecasted ? (
                <>
                  Nguồn dữ liệu: {dataSource}
                  {selectedMedicine && (
                    <span className="ml-4">
                      • Thuốc: {selectedMedicine.name} ({selectedMedicine.manufacturer}) • Vùng: {selectedRegion}
                    </span>
                  )}
                </>
              ) : (
                "Chọn thuốc và vùng miền để bắt đầu dự báo"
              )}
            </Text>
          </div>

          <div className="flex items-center gap-2">
            <Button
              size="small"
              icon={<DownloadOutlined />}
              className="rounded-full border border-slate-200 bg-white text-slate-700 shadow-sm hover:bg-slate-50"
            >
              Xuất
            </Button>
          </div>
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-3">
          {/* Select thuốc */}
          <div>
            <Text className="text-sm font-medium text-slate-700 mb-2 block">
              Chọn thuốc:
            </Text>
            <Select
              value={selectedMedicineId}
              onChange={(value) => {
                setSelectedMedicineId(value);
                setHasForecasted(false);
              }}
              placeholder="Chọn thuốc để xem dự báo"
              className="w-full"
              loading={medicines.length === 0}
              disabled={loading}
              showSearch
              optionFilterProp="children"
            >
              {medicines.map((medicine) => (
                <Select.Option key={medicine.medicineId} value={medicine.medicineId}>
                  {medicine.name} - {medicine.manufacturer}
                </Select.Option>
              ))}
            </Select>
          </div>

          {/* Select vùng */}
          <div>
            <Text className="text-sm font-medium text-slate-700 mb-2 block">
              Chọn vùng miền:
            </Text>
            <Select
              value={selectedRegion}
              onChange={(value) => {
                setSelectedRegion(value);
                setHasForecasted(false);
              }}
              placeholder="Chọn vùng miền"
              className="w-full"
              disabled={loading}
            >
              <Select.Option value="Bắc">Miền Bắc</Select.Option>
              <Select.Option value="Trung">Miền Trung</Select.Option>
              <Select.Option value="Nam">Miền Nam</Select.Option>
            </Select>
          </div>

          {/* Button */}
          <div className="flex items-end">
            <Button
              type="primary"
              onClick={handleForecast}
              loading={loading}
              disabled={!selectedMedicineId}
              className="w-full h-[40px] rounded-lg text-base font-semibold shadow-lg"
            >
              {loading ? 'Đang dự báo...' : 'Dự báo'}
            </Button>
          </div>
        </div>
      </div>

      {/* Chart */}
      <div className="mb-5 rounded-xl border border-emerald-100 bg-linear-to-br from-[#f0fdf4] to-[#f8fafc] p-4 shadow-lg">
        {!hasForecasted ? (
          <div className="flex h-130 items-center justify-center text-slate-400">
            <div className="text-center">
              <div className="text-4xl mb-4">📊</div>
              <div className="text-lg font-medium mb-2">Chưa có dữ liệu dự báo</div>
              <div className="text-sm">Vui lòng chọn thuốc và vùng miền, sau đó nhấn "Dự báo"</div>
            </div>
          </div>
        ) : loading ? (
          <div className="flex h-130 items-center justify-center text-slate-400">
            <span>Đang tải dữ liệu AI...</span>
          </div>
        ) : error ? (
          <div className="flex h-130 items-center justify-center text-red-400">
            <span>{error}</span>
          </div>
        ) : chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height={520}>
            <ComposedChart data={chartData} margin={{ top: 20, right: 60, left: 60, bottom: 80 }}>
              <defs>
                <linearGradient id="forecasrBandGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.2} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="4 4" stroke="#d1d5db" vertical={true} opacity={0.5} />
              <XAxis
                dataKey="date"
                stroke="#6b7280"
                fontSize={12}
                tick={{ fontSize: 12 }}
                angle={-45}
                textAnchor="end"
                height={100}
              />
              {/* Single Y-Axis for history and forecast */}
              <YAxis
                yAxisId="left"
                stroke="#3b82f6"
                fontSize={12}
                tick={{ fontSize: 12, fill: '#3b82f6' }}
                tickLine={{ stroke: '#cbd5e1' }}
                axisLine={{ stroke: '#cbd5e1' }}
                domain={[ 'auto', 'auto' ]}
                tickCount={6}
                label={{ value: 'Đơn vị', angle: -90, position: 'insideLeft', offset: -10, style: { fontSize: 12, fontWeight: 600, fill: '#3b82f6' } }}
                width={55}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#fff',
                  border: '2px solid #e2e8f0',
                  borderRadius: '10px',
                  boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
                  padding: '12px'
                }}
                formatter={(value, name) => {
                  if (value === null || value === undefined) return ['—', name];
                  const formatted = Math.round(value as number);
                  return [formatted.toLocaleString('vi-VN'), name];
                }}
                labelFormatter={(label) => `${label}`}
                labelStyle={{ color: '#000', fontWeight: 'bold', marginBottom: '8px' }}
                separator={': '}
              />
              <Legend
                wrapperStyle={{ paddingTop: '20px' }}
                iconType="line"
                height={36}
              />
              {/* Combined history + forecast line */}
              <Line
                yAxisId="left"
                type="natural"
                dataKey={(entry) => entry.predicted ?? entry.forecast}
                stroke="#3b82f6"
                strokeWidth={2}
                name="Lịch sử & Dự báo"
                dot={{ fill: '#3b82f6', r: 3, strokeWidth: 1, stroke: '#fff' }}
                activeDot={{ r: 6, strokeWidth: 1 }}
                connectNulls={true}
                isAnimationActive={true}
              />

              {/* Forecast area band */}
              <Area
                yAxisId="left"
                type="natural"
                dataKey="upper"
                fill="#10b981"
                stroke="none"
                fillOpacity={0.1}
                isAnimationActive={true}
                name="Vùng dự báo"
              />

              {/* Upper bound - dashed */}
              <Line
                yAxisId="left"
                type="natural"
                dataKey="upper"
                stroke="#f59e0b"
                strokeWidth={1.5}
                strokeDasharray="6 4"
                name="Khoảng trên"
                dot={false}
                isAnimationActive={true}
                opacity={0.9}
              />

              {/* Lower bound - dashed */}
              <Line
                yAxisId="left"
                type="natural"
                dataKey="lower"
                stroke="#ef4444"
                strokeWidth={1.5}
                strokeDasharray="6 4"
                name="Khoảng dưới"
                dot={false}
                isAnimationActive={true}
                opacity={0.9}
              />

              {/* Today divider line */}
              {showTodayLine && (
                <ReferenceLine
                  x={todayDate}
                  stroke="#ef4444"
                  strokeWidth={2}
                  strokeDasharray="8 4"
                  isFront={true}
                  label={{ value: 'Hôm nay', position: 'insideTopRight', fill: '#ef4444', fontWeight: 'bold' }}
                />
              )}
            </ComposedChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex h-130 items-center justify-center text-slate-400">
            <span>Chưa có dữ liệu dự báo AI. Vui lòng setup backend và AI service.</span>
          </div>
        )}
      </div>

      {/* Metrics */}
      <Row gutter={[12, 12]}>
        {computedMetrics.map((metric) => (
          <Col key={metric.label} xs={24} sm={8}>
            <div
              className={`flex flex-col gap-1.5 rounded-xl p-3 ${metric.bg}`}
            >
              <Text className="text-xs text-slate-500">
                {metric.label}
              </Text>

              <div className={`text-sm font-semibold ${metric.color}`}>
                {metric.value}
              </div>
            </div>
          </Col>
        ))}
      </Row>
    </Card>
  );
}

export default ForecastPanel;
