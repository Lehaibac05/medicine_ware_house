import { Card, Col, Row, Typography, Spin, message } from "antd";
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  MedicineBoxOutlined,
  WarningOutlined,
  InboxOutlined,
  FileDoneOutlined,
} from "@ant-design/icons";
import { useEffect, useState } from "react";
import { reportApi } from "../../../services/reports";
import { getMedicineRequestsPage } from "../../../services/medicineRequests";

const { Text } = Typography;

type StatItem = {
  label: string;
  value: number;
  note: string;
  icon: React.ReactNode;
  trend: string;
  trendUp: boolean;
  color: string;
  bg: string;
};

function StatsGrid() {
  const [stats, setStats] = useState<StatItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    const loadStats = async () => {
      try {
        setLoading(true);
        const [dashboardData, pendingRequests] = await Promise.all([
          reportApi.getDashboardSummary(),
          getMedicineRequestsPage({ status: "PENDING", page: 0, size: 1 }),
        ]);

        const statsData: StatItem[] = [
          {
            label: "Tổng số thuốc",
            value: dashboardData.totalMedicines,
            note: `Tồn kho: ${dashboardData.totalStock.toLocaleString()}`,
            icon: <MedicineBoxOutlined />,
            trend: "+12%",
            trendUp: true,
            color: "text-emerald-600",
            bg: "bg-[#f0fdf4]",
          },
          {
            label: "Sắp hết hạn",
            value: dashboardData.expiringSoonCount,
            note: "Cần ưu tiên xuất",
            icon: <WarningOutlined />,
            trend: "-5%",
            trendUp: false,
            color: "text-amber-600",
            bg: "bg-[#fffbeb]",
          },
          {
            label: "Tồn kho thấp",
            value: dashboardData.lowStockCount,
            note: "Dưới mức an toàn",
            icon: <InboxOutlined />,
            trend: "+3%",
            trendUp: true,
            color: "text-red-600",
            bg: "bg-[#fef2f2]",
          },
          {
            label: "Đơn chờ duyệt",
            value: pendingRequests.totalElements,
            note: "Chờ phê duyệt",
            icon: <FileDoneOutlined />,
            trend: "+8%",
            trendUp: true,
            color: "text-blue-600",
            bg: "bg-[#eff6ff]",
          },
        ];

        setStats(statsData);
      } catch (error) {
        messageApi.error("Không thể tải dữ liệu thống kê");
        console.error(error);
      } finally {
        setLoading(false);
      }
    };

    void loadStats();
  }, [messageApi]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-48">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <>
      {contextHolder}
      <Row gutter={[16, 16]}>
        {stats.map((stat) => (
          <Col key={stat.label} xs={24} sm={12} xl={6}>
            <Card
              className={`group relative overflow-hidden !rounded-2xl !border-0 ${stat.bg}
              transition duration-200 hover:-translate-y-1 hover:shadow-[0_20px_40px_rgba(0,0,0,0.06)]`}
            >
              {/* subtle glow */}
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

              {/* Value */}
              <div className="mt-3 flex items-end justify-between">
                <div className={`text-3xl font-extrabold ${stat.color}`}>
                  {stat.value.toLocaleString()}
                </div>

                {/* Trend */}
                <div
                  className={`flex items-center gap-1 text-xs font-semibold ${
                    stat.trendUp ? "text-emerald-600" : "text-red-500"
                  }`}
                >
                  {stat.trendUp ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                  {stat.trend}
                </div>
              </div>

              {/* Note */}
              <Text className="mt-2 block text-[13px] text-slate-500">
                {stat.note}
              </Text>
            </Card>
          </Col>
        ))}
      </Row>
    </>
  );
}

export default StatsGrid;