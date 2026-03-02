import { Button, Card, Col, Row, Tag, Typography } from "antd";

const { Text, Title } = Typography;

const metrics = [
  { label: "Tuần này", value: "+12%", color: "geekblue" },
  { label: "Mức rủi ro", value: "Thấp", color: "green" },
  { label: "Đề xuất nhập", value: "18 mặt hàng", color: "gold" },
];

function ForecastPanel() {
  return (
    <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            AI Demand Forecast
          </Text>
          <Title level={4} className="!m-0">
            Dự báo nhu cầu 30 ngày
          </Title>
        </div>
        <Button size="small">Xuất báo cáo</Button>
      </div>
      <div className="mb-4 grid h-[220px] place-items-center rounded-xl border border-dashed border-indigo-200 bg-slate-50 text-slate-400">
        Biểu đồ dự báo AI
      </div>
      <Row gutter={[12, 12]}>
        {metrics.map((metric) => (
          <Col key={metric.label} xs={24} sm={8}>
            <div className="flex flex-col gap-1.5 rounded-xl bg-slate-50 p-3">
              <Text className="text-xs text-slate-500">{metric.label}</Text>
              <Tag color={metric.color} className="!m-0">
                {metric.value}
              </Tag>
            </div>
          </Col>
        ))}
      </Row>
    </Card>
  );
}

export default ForecastPanel;
