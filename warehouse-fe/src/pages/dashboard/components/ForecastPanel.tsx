// import { Button, Card, Col, Row, Tag, Typography } from "antd";

// const { Text, Title } = Typography;

// const metrics = [
//   { label: "Tuần này", value: "+12%", color: "geekblue" },
//   { label: "Mức rủi ro", value: "Thấp", color: "green" },
//   { label: "Đề xuất nhập", value: "18 mặt hàng", color: "gold" },
// ];

// function ForecastPanel() {
//   return (
//     <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
//       <div className="mb-4 flex items-start justify-between gap-4">
//         <div>
//           <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
//             AI Demand Forecast
//           </Text>
//           <Title level={4} className="!m-0">
//             Dự báo nhu cầu 30 ngày
//           </Title>
//         </div>
//         <Button size="small">Xuất báo cáo</Button>
//       </div>
//       <div className="mb-4 grid h-[220px] place-items-center rounded-xl border border-dashed border-indigo-200 bg-slate-50 text-slate-400">
//         Biểu đồ dự báo AI
//       </div>
//       <Row gutter={[12, 12]}>
//         {metrics.map((metric) => (
//           <Col key={metric.label} xs={24} sm={8}>
//             <div className="flex flex-col gap-1.5 rounded-xl bg-slate-50 p-3">
//               <Text className="text-xs text-slate-500">{metric.label}</Text>
//               <Tag color={metric.color} className="!m-0">
//                 {metric.value}
//               </Tag>
//             </div>
//           </Col>
//         ))}
//       </Row>
//     </Card>
//   );
// }

// export default ForecastPanel;

import { Button, Card, Col, Row, Typography } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from "recharts";
import { useEffect, useMemo, useState } from "react";
import { forecastApi, type ForecastPoint } from "../../../services/forecast";

const { Text, Title } = Typography;

function ForecastPanel() {
  const [chartData, setChartData] = useState<ForecastPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [usingMockData, setUsingMockData] = useState(false);

  const computedMetrics = useMemo(() => {    
    if (error) {
      return [
        { label: "Tuần này", value: "Lỗi", color: "text-red-600", bg: "bg-red-100" },
        { label: "Mức rủi ro", value: "Lỗi", color: "text-red-600", bg: "bg-red-100" },
        { label: "Đề xuất nhập", value: "Lỗi", color: "text-red-600", bg: "bg-red-100" },
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

    const weekStart = recent[0]?.predicted || lastPoint.predicted;
    const weekEnd = lastPoint.predicted;
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
  }, [chartData, error]);

  useEffect(() => {
    const loadData = async () => {
      try {
        setError(null);
        setUsingMockData(false);
        const data = await forecastApi.get30DayForecast();
        
        // Check if this is mock data (has consistent pattern)
        const isMock = data.some(item => item.isFallback);
        setUsingMockData(isMock);
        
        // Data is already processed in forecast.ts, just set it
        setChartData(data);
      } catch (error) {
        console.error('Error loading forecast:', error);
        setError('Không thể tải dữ liệu dự báo AI');
        setChartData([]);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  return (
    <Card className="rounded-2xl! border-0! shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      
      {/* Header */}
      <div className="mb-5 flex items-start justify-between gap-4">
        <div>
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            AI Forecast
          </Text>

          <Title level={4} className="m-0! mt-1! font-semibold">
            Dự báo nhu cầu 30 ngày {usingMockData && <span className="text-amber-500 text-sm">(Demo)</span>}
          </Title>
        </div>

        <Button
          size="small"
          icon={<DownloadOutlined />}
          className="rounded-full!"
        >
          Xuất
        </Button>
      </div>

      {/* Chart */}
      <div className="mb-5 rounded-xl border border-emerald-100 bg-linear-to-br from-[#f0fdf4] to-[#f8fafc] p-4">
        {loading ? (
          <div className="flex h-[350px] items-center justify-center text-slate-400">
            <span>Đang tải dữ liệu AI...</span>
          </div>
        ) : error ? (
          <div className="flex h-[350px] items-center justify-center text-red-400">
            <span>{error}</span>
          </div>
        ) : chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height={350}>
            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} />
              <YAxis stroke="#94a3b8" fontSize={12} />
              <Tooltip 
                contentStyle={{ backgroundColor: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px' }}
                formatter={(value) => [Math.round(value as number), 'Số lượng']}
                labelStyle={{ color: '#000' }}
              />
              <Legend />
              {/* History line - blue */}
              <Line 
                type="monotone" 
                dataKey="predicted" 
                stroke="#3b82f6" 
                strokeWidth={3}
                name="Dữ liệu lịch sử" 
                dot={{ fill: '#3b82f6', r: 4 }}
                activeDot={{ r: 6 }}
                connectNulls={false}
              />
              {/* Forecast line - green */}
              <Line 
                type="monotone" 
                dataKey="forecast" 
                stroke="#10b981" 
                strokeWidth={3}
                name="Dự báo AI" 
                dot={{ fill: '#10b981', r: 4 }}
                activeDot={{ r: 6 }}
                connectNulls={false}
              />
              <Line 
                type="monotone" 
                dataKey="upper" 
                stroke="#fbbf24" 
                strokeWidth={1.5}
                strokeDasharray="5 5" 
                name="Khoảng trên" 
                dot={false}
              />
              <Line 
                type="monotone" 
                dataKey="lower" 
                stroke="#f87171" 
                strokeWidth={1.5}
                strokeDasharray="5 5" 
                name="Khoảng dưới" 
                dot={false}
              />
              {/* Today divider line */}
              <ReferenceLine 
                x={new Date().toISOString().split('T')[0]} 
                stroke="#ef4444" 
                strokeWidth={2}
                strokeDasharray="8 4"
                label="Hôm nay"
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex h-[350px] items-center justify-center text-slate-400">
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
