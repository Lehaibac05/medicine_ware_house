import { Card, Col, Row, Typography, Spin } from "antd";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import {
  ShoppingCartOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DollarOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from "@ant-design/icons";
import { getOrders } from "../../../services/orders";

const { Text } = Typography;

type StatItem = {
  label: string;
  value: number | string;
  note?: ReactNode;
  icon?: ReactNode;
  color?: string;
  bg?: string;
  trend?: string;
  trendUp?: boolean;
};

function OrdersStatsGrid() {
  const [stats, setStats] = useState<StatItem[]>([
    {
      label: "Đơn chờ xử lý",
      value: 0,
      note: "Đang chờ xử lý",
      icon: <ShoppingCartOutlined />,
      color: "text-amber-600",
      bg: "bg-[#fffbeb]",
    },
    {
      label: "Đơn hoàn thành",
      value: 0,
      note: "Đã giao thành công",
      icon: <CheckCircleOutlined />,
      color: "text-emerald-600",
      bg: "bg-[#f0fdf4]",
    },
    {
      label: "Đơn đã hủy",
      value: 0,
      note: "Đã hủy hoặc từ chối",
      icon: <CloseCircleOutlined />,
      color: "text-red-600",
      bg: "bg-[#fef2f2]",
    },
    {
      label: "Tổng doanh thu",
      value: 0,
      note: "Từ các đơn hoàn thành",
      icon: <DollarOutlined />,
      color: "text-blue-600",
      bg: "bg-[#eff6ff]",
    },
  ]);

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      setLoading(true);
      const orders = await getOrders();

      const pending = orders.filter((o) => o.status === "PENDING").length;
      const completed = orders.filter((o) => o.status === "COMPLETED").length;
      const cancelled = orders.filter((o) => o.status === "CANCELLED").length;

      const totalRevenue = orders
        .filter((o) => o.status === "COMPLETED")
        .reduce((sum, order) => sum + order.totalAmount, 0);

      setStats([
        {
          label: "Đơn chờ xử lý",
          value: pending,
          note: "Đang chờ xử lý",
          icon: <ShoppingCartOutlined />,
          color: "text-amber-600",
          bg: "bg-[#fffbeb]",
          trend: "+4%",
          trendUp: true,
        },
        {
          label: "Đơn hoàn thành",
          value: completed,
          note: "Đã giao thành công",
          icon: <CheckCircleOutlined />,
          color: "text-emerald-600",
          bg: "bg-[#f0fdf4]",
          trend: "+6%",
          trendUp: true,
        },
        {
          label: "Đơn đã hủy",
          value: cancelled,
          note: "Đã hủy hoặc từ chối",
          icon: <CloseCircleOutlined />,
          color: "text-red-600",
          bg: "bg-[#fef2f2]",
          trend: "-2%",
          trendUp: false,
        },
        {
          label: "Tổng doanh thu",
          value: totalRevenue,
          note: "Từ các đơn hoàn thành",
          icon: <DollarOutlined />,
          color: "text-blue-600",
          bg: "bg-[#eff6ff]",
          trend: "+8%",
          trendUp: true,
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
                {typeof stat.value === "number"
                  ? stat.value.toLocaleString("vi-VN")
                  : stat.value}
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

export default OrdersStatsGrid;