import { Button, Card, Col, Row, Select, Typography } from "antd";
import {
    ComposedChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    ResponsiveContainer,
    Tooltip,
    ReferenceLine,
    Area,
    Legend,
} from "recharts";
import { useEffect, useMemo, useState } from "react";
import { forecastApi, type ForecastPoint } from "../../../services/forecast";
import { getMedicines, type Medicine } from "../../../services/medicines";

const { Title } = Typography;

const DashboardForecastPanel = () => {
    const [chartData, setChartData] = useState<ForecastPoint[]>([]);
    const [medicines, setMedicines] = useState<Medicine[]>([]);
    const [selectedMedicineId, setSelectedMedicineId] = useState<number>();
    const [selectedRegion, setSelectedRegion] = useState<string>("Bắc");

    const [loading, setLoading] = useState(false);
    const [error, setErrorState] = useState<string | null>(null);

    const todayDate = new Date().toISOString().split("T")[0];

    // ================= FETCH =================
    const fetchForecast = async () => {
        if (!selectedMedicineId) return;

        setLoading(true);
        setErrorState(null);

        try {
            const data = await forecastApi.get30DayForecast(
                selectedMedicineId,
                selectedRegion
            );

            // normalize data (tránh lặp logic)
            const normalized = data.map((d) => ({
                ...d,
                value: d.predicted ?? d.forecast ?? 0,
            }));

            // Sắp xếp dữ liệu theo ngày
            const sortedData = normalized.sort((a, b) => a.date.localeCompare(b.date));

            // Lấy 30 ngày gần nhất có dữ liệu
            const recentData = sortedData.slice(-30);

            setChartData(recentData);
        } catch (err) {
            console.error(err);
            setErrorState("Không thể tải dữ liệu dự báo");
            setChartData([]);
        } finally {
            setLoading(false);
        }
    };

    // ================= LOAD MEDICINES =================
    useEffect(() => {
        const load = async () => {
            try {
                const res = await getMedicines({
                    page: 0,
                    size: 100,
                    sortBy: "name",
                    sortDir: "asc",
                });

                setMedicines(res.content);
                if (res.content.length) {
                    setSelectedMedicineId(res.content[0].medicineId);
                }
            } catch (err) {
                console.error(err);
            }
        };

        load();
    }, []);

    const showTodayLine = useMemo(
        () => chartData.some((p) => p.date?.startsWith(todayDate)),
        [chartData, todayDate]
    );

    // ================= UI =================
    return (
        <Card className="rounded-2xl shadow bg-white" bodyStyle={{ padding: 24 }}>
            {/* HEADER */}
            <div className="mb-5 p-5 bg-slate-50 rounded-2xl border">
                <div>
                    <Title level={4}>Dự báo nhu cầu 30 ngày</Title>
                </div>
                <div className="flex flex-col lg:flex-row gap-4 justify-between">

                    <div className="w-full max-w-2xl">
                        <Row gutter={12}>
                            <Col span={10}>
                                <Select
                                    value={selectedMedicineId}
                                    onChange={setSelectedMedicineId}
                                    className="w-full"
                                    loading={!medicines.length}
                                    disabled={loading}
                                    showSearch
                                >
                                    {medicines.map((m) => (
                                        <Select.Option key={m.medicineId} value={m.medicineId}>
                                            {m.name}
                                        </Select.Option>
                                    ))}
                                </Select>
                            </Col>

                            <Col span={7}>
                                <Select
                                    value={selectedRegion}
                                    onChange={setSelectedRegion}
                                    className="w-full"
                                    disabled={loading}
                                >
                                    <Select.Option value="Bắc">Bắc</Select.Option>
                                    <Select.Option value="Trung">Trung</Select.Option>
                                    <Select.Option value="Nam">Nam</Select.Option>
                                </Select>
                            </Col>

                            <Col span={7}>
                                <Button
                                    type="primary"
                                    onClick={fetchForecast}
                                    loading={loading}
                                    block
                                >
                                    Dự báo
                                </Button>
                            </Col>
                        </Row>
                    </div>
                </div>
            </div>

            {/* CHART */}
            <div style={{ height: 360 }}>
                {loading ? (
                    <div className="h-full flex items-center justify-center">
                        Loading...
                    </div>
                ) : error ? (
                    <div className="text-red-500 text-center">{error}</div>
                ) : chartData.length ? (
                    <ResponsiveContainer width="100%" height="100%">
                        <ComposedChart data={chartData} margin={{ top: 20, right: 40, left: 0, bottom: 40 }}>
                            <defs>
                                <linearGradient id="forecastBandGradient" x1="0" y1="0" x2="0" y2="1">
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
                                height={70}
                            />
                            <YAxis
                                yAxisId="left"
                                stroke="#3b82f6"
                                fontSize={12}
                                tick={{ fontSize: 12, fill: '#3b82f6' }}
                                tickLine={{ stroke: '#cbd5e1' }}
                                axisLine={{ stroke: '#cbd5e1' }}
                                domain={['auto', 'auto']}
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

                            {/* Forecast line */}
                            <Line
                                yAxisId="left"
                                type="natural"
                                dataKey="forecast"
                                stroke="#10b981"
                                strokeWidth={2}
                                name="Dự báo AI"
                                dot={{ fill: '#10b981', r: 3, strokeWidth: 1, stroke: '#fff' }}
                                activeDot={{ r: 6, strokeWidth: 1 }}
                                connectNulls={true}
                                isAnimationActive={true}
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
                                    label={{ value: 'Hôm nay', position: 'insideTopRight', fill: '#ef4444', fontWeight: 'bold' }}
                                />
                            )}
                        </ComposedChart>
                    </ResponsiveContainer>
                ) : (
                    <div className="text-center text-slate-400">
                        Chưa có dữ liệu
                    </div>
                )}
            </div>
        </Card>
    );
};

export default DashboardForecastPanel;