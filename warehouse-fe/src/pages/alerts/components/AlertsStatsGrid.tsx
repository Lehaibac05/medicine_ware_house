import { Card, Col, Row, Typography, Spin } from "antd";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import {
  WarningOutlined,
  ClockCircleOutlined,
  AlertOutlined,
  StopOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from "@ant-design/icons";
import { getAlertStats, type AlertStats } from "../../../services/alerts";

const { Text } = Typography;

type StatItem = {
  label: string;
  value: number;
  note?: ReactNode;
  icon?: ReactNode;
  color?: string;
  bg?: string;
  trend?: string;
  trendUp?: boolean;
};

function AlertsStatsGrid() {
  const [stats, setStats] = useState<StatItem[]>([
    {
      label: "Cảnh báo tồn kho thấp",
      value: 0,
      note: "Dưới mức an toàn",
      icon: <WarningOutlined />,
      color: "text-red-600",
      bg: "bg-[#fef2f2]",
    },
    {
      label: "Sắp hết hạn",
      value: 0,
      note: "Trong 30 ngày tới",
      icon: <ClockCircleOutlined />,
      color: "text-amber-600",
      bg: "bg-[#fffbeb]",
    },
    {
      label: "Cảnh báo hệ thống",
      value: 0,
      note: "Tích hợp & tiến trình",
      icon: <AlertOutlined />,
      color: "text-blue-600",
      bg: "bg-[#eff6ff]",
    },
    {
      label: "Đã hết hạn",
      value: 0,
      note: "Cần xử lý",
      icon: <StopOutlined />,
      color: "text-slate-600",
      bg: "bg-[#f1f5f9]",
    },
  ]);

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      setLoading(true);
      const data: AlertStats = await getAlertStats();

      setStats([
        {
          label: "Cảnh báo tồn kho thấp",
          value: data.lowStockCount,
          note: "Dưới mức an toàn",
          icon: <WarningOutlined />,
          color: "text-red-600",
          bg: "bg-[#fef2f2]",
          trend: "+5%",
          trendUp: true,
        },
        {
          label: "Sắp hết hạn",
          value: data.expiringSoonCount,
          note: "Trong 30 ngày tới",
          icon: <ClockCircleOutlined />,
          color: "text-amber-600",
          bg: "bg-[#fffbeb]",
          trend: "+2%",
          trendUp: false,
        },
        {
          label: "Cảnh báo hệ thống",
          value: data.systemWarningsCount,
          note: "Tích hợp & tiến trình",
          icon: <AlertOutlined />,
          color: "text-blue-600",
          bg: "bg-[#eff6ff]",
          trend: "+1%",
          trendUp: true,
        },
        {
          label: "Đã hết hạn",
          value: data.expiredCount,
          note: "Cần xử lý",
          icon: <StopOutlined />,
          color: "text-slate-600",
          bg: "bg-[#f1f5f9]",
          trend: "-3%",
          trendUp: false,
        },
      ]);
    } catch (error) {
      console.error("Load stats error:", error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-8">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <Row gutter={[16, 16]}>
      {stats.map((stat) => (
        <Col key={stat.label} xs={24} sm={12} xl={6}>
          <Card
            className={`group relative overflow-hidden !rounded-2xl !border-0 ${
              stat.bg
            }
            transition duration-200 hover:-translate-y-1 hover:shadow-[0_20px_40px_rgba(0,0,0,0.06)]`}
          >
            {/* glow */}
            <div className="pointer-events-none absolute -right-10 -top-10 h-24 w-24 rounded-full bg-white/40 blur-2xl opacity-0 group-hover:opacity-100 transition" />

            {/* Top */}
            <div className="flex items-start justify-between">
              <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                {stat.label}
              </Text>

              <div className="text-slate-300 text-lg group-hover:scale-110 transition">
                {stat.icon}
              </div>
            </div>

            {/* Value + Trend */}
            <div className="mt-3 flex items-end justify-between">
              <div className={`text-3xl font-extrabold ${stat.color}`}>
                {stat.value.toLocaleString()}
              </div>

              {stat.trend && (
                <div
                  className={`flex items-center gap-1 text-xs font-semibold ${
                    stat.trendUp
                      ? "text-emerald-600"
                      : "text-red-500"
                  }`}
                >
                  {stat.trendUp ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                  {stat.trend}
                </div>
              )}
            </div>

            {/* Note */}
            {stat.note && (
              <Text className="mt-2 block text-[13px] text-slate-500">
                {stat.note}
              </Text>
            )}
          </Card>
        </Col>
      ))}
    </Row>
  );
}

export default AlertsStatsGrid;