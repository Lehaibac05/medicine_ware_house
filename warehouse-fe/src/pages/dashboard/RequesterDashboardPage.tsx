import { Card, Col, Empty, Row, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import MainLayout from "../../layouts/MainLayout";
import {
  getMyIssueRequests,
  type IssueRequest,
  type IssueStatus,
} from "../../services/issue";

const { Text, Title } = Typography;

type RequesterStat = {
  label: string;
  value: number;
  note: string;
  colorClass: string;
  bgClass: string;
};

const statusColor = (status: IssueStatus) => {
  if (status === "APPROVED") return "green";
  if (status === "PENDING") return "gold";
  if (status === "REJECTED") return "red";
  return "blue";
};

const RequesterDashboardPage = () => {
  const [loading, setLoading] = useState(false);
  const [requests, setRequests] = useState<IssueRequest[]>([]);

  useEffect(() => {
    const loadMyRequests = async () => {
      setLoading(true);
      try {
        const data = await getMyIssueRequests();
        setRequests(data);
      } catch {
        setRequests([]);
      } finally {
        setLoading(false);
      }
    };

    void loadMyRequests();
  }, []);

  const stats = useMemo<RequesterStat[]>(() => {
    const pending = requests.filter((item) => item.status === "PENDING").length;
    const approved = requests.filter((item) => item.status === "APPROVED").length;
    const rejected = requests.filter((item) => item.status === "REJECTED").length;
    const completed = requests.filter((item) => item.status === "COMPLETED").length;

    return [
      {
        label: "Tổng yêu cầu",
        value: requests.length,
        note: "Toàn bộ yêu cầu của bạn",
        colorClass: "text-blue-600",
        bgClass: "bg-[#eff6ff]",
      },
      {
        label: "Đang chờ duyệt",
        value: pending,
        note: "Chờ quản lý xử lý",
        colorClass: "text-amber-600",
        bgClass: "bg-[#fffbeb]",
      },
      {
        label: "Đã duyệt",
        value: approved,
        note: "Yêu cầu đã được duyệt",
        colorClass: "text-emerald-600",
        bgClass: "bg-[#f0fdf4]",
      },
      {
        label: "Bị từ chối",
        value: rejected,
        note: "Cần tạo lại hoặc điều chỉnh",
        colorClass: "text-red-600",
        bgClass: "bg-[#fef2f2]",
      },
      {
        label: "Đã hoàn tất",
        value: completed,
        note: "Đã cấp thuốc xong",
        colorClass: "text-cyan-600",
        bgClass: "bg-[#ecfeff]",
      },
    ];
  }, [requests]);

  const recentRequests = useMemo(() => {
    return [...requests]
      .sort((a, b) => {
        const timeA = new Date(a.createdAt || a.requestDate || 0).getTime();
        const timeB = new Date(b.createdAt || b.requestDate || 0).getTime();
        return timeB - timeA;
      })
      .slice(0, 8);
  }, [requests]);

  const columns: ColumnsType<IssueRequest> = [
    {
      title: "Mã yêu cầu",
      dataIndex: "orderId",
      width: 110,
      render: (value: number) => <Text strong>#{value}</Text>,
    },
    { title: "Thuốc", dataIndex: "medicineName" },
    {
      title: "Số lượng",
      dataIndex: "requestedQuantity",
      width: 110,
      render: (value?: number) => Number(value || 0).toLocaleString(),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 140,
      render: (value: IssueStatus) => <Tag color={statusColor(value)}>{value}</Tag>,
    },
    {
      title: "Ngày tạo",
      dataIndex: "createdAt",
      width: 170,
      render: (value?: string, record?: IssueRequest) => {
        const dateValue = value || record?.requestDate;
        return dateValue ? new Date(dateValue).toLocaleString() : "-";
      },
    },
  ];

  return (
    <MainLayout>
      <div>
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Requester Dashboard
        </Text>
        <Title level={4} className="!mt-1 !mb-0 font-semibold">
          Tổng quan yêu cầu cấp thuốc của tôi
        </Title>
      </div>

      <Row gutter={[16, 16]}>
        {stats.map((stat) => (
          <Col key={stat.label} xs={24} sm={12} xl={8}>
            <Card className={`!rounded-2xl !border-0 ${stat.bgClass}`}>
              <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                {stat.label}
              </Text>
              <div className={`mt-2 text-3xl font-extrabold ${stat.colorClass}`}>
                {stat.value.toLocaleString()}
              </div>
              <Text className="mt-1 block text-[13px] text-slate-500">{stat.note}</Text>
            </Card>
          </Col>
        ))}
      </Row>

      <Card className="!rounded-2xl !border-0 shadow-[0_10px_24px_rgba(15,23,42,0.06)]">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Recent Requests
        </Text>
        <Title level={4} className="!mt-1 !mb-4 font-semibold">
          Yêu cầu gần đây
        </Title>

        {recentRequests.length === 0 && !loading ? (
          <Empty description="Bạn chưa có yêu cầu cấp thuốc" />
        ) : (
          <Table<IssueRequest>
            rowKey="orderId"
            columns={columns}
            dataSource={recentRequests}
            loading={loading}
            pagination={false}
            scroll={{ x: 800 }}
          />
        )}
      </Card>
    </MainLayout>
  );
};

export default RequesterDashboardPage;
