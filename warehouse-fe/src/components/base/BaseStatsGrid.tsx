import { Card, Col, Row, Statistic, Typography } from "antd";

const { Text } = Typography;

export type StatItem = {
  label: string;
  value: number | string;
  note?: string;
};

type BaseStatsGridProps = {
  stats: StatItem[];
};

function BaseStatsGrid({ stats }: BaseStatsGridProps) {
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
              valueStyle={{
                fontSize: 28,
                fontWeight: 700,
                color: "#0f172a",
              }}
            />

            {stat.note && (
              <Text className="text-[13px] text-slate-500">
                {stat.note}
              </Text>
            )}
          </Card>
        </Col>
      ))}
    </Row>
  );
}

export default BaseStatsGrid;