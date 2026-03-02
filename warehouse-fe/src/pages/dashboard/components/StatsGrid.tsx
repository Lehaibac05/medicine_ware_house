import { Card, Col, Row, Statistic, Typography } from "antd";

const { Text } = Typography;

const stats = [
  { label: "Tổng số thuốc", value: 12480, note: "32 mới nhập hôm nay" },
  { label: "Sắp hết hạn", value: 186, note: "Cần ưu tiên xuất" },
  { label: "Tồn kho thấp", value: 42, note: "Dưới mức an toàn" },
  { label: "Đơn chờ duyệt", value: 28, note: "7 đơn khẩn" },
];

function StatsGrid() {
  return (
    <Row gutter={[16, 16]}>
      {stats.map((stat) => (
        <Col key={stat.label} xs={24} sm={12} xl={6}>
          <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              {stat.label}
            </Text>
            <Statistic
              value={stat.value}
              valueStyle={{ fontSize: 28, fontWeight: 700, color: "#0f172a" }}
            />
            <Text className="text-[13px] text-slate-500">{stat.note}</Text>
          </Card>
        </Col>
      ))}
    </Row>
  );
}

export default StatsGrid;
