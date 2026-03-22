// import { Card, Col, Row, Statistic, Typography } from "antd";

// const { Text } = Typography;

// export type StatItem = {
//   label: string;
//   value: number | string;
//   note?: string;
// };

// type BaseStatsGridProps = {
//   stats: StatItem[];
// };

// function BaseStatsGrid({ stats }: BaseStatsGridProps) {
//   return (
//     <Row gutter={[16, 16]}>
//       {stats.map((stat) => (
//         <Col key={stat.label} xs={24} sm={12} xl={6}>
//           <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
//             <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
//               {stat.label}
//             </Text>

//             <Statistic
//               value={stat.value}
//               valueStyle={{
//                 fontSize: 28,
//                 fontWeight: 700,
//                 color: "#0f172a",
//               }}
//             />

//             {stat.note && (
//               <Text className="text-[13px] text-slate-500">
//                 {stat.note}
//               </Text>
//             )}
//           </Card>
//         </Col>
//       ))}
//     </Row>
//   );
// }

// export default BaseStatsGrid;

import { Card, Col, Row, Typography } from "antd";
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
} from "@ant-design/icons";

const { Text } = Typography;

export type StatItem = {
  label: string;
  value: number | string;
  note?: string;
  icon?: React.ReactNode;
  trend?: string;
  trendUp?: boolean;
  color?: string;
  bg?: string;
};

type BaseStatsGridProps = {
  stats: StatItem[];
};

function BaseStatsGrid({ stats }: BaseStatsGridProps) {
  return (
    <Row gutter={[16, 16]}>
      {stats.map((stat) => (
        <Col key={stat.label} xs={24} sm={12} xl={6}>
          <Card
            className={`group relative overflow-hidden !rounded-2xl !border-0 ${
              stat.bg || "bg-white"
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

              {stat.icon && (
                <div className="text-slate-300 text-lg group-hover:scale-110 transition">
                  {stat.icon}
                </div>
              )}
            </div>

            {/* Value + Trend */}
            <div className="mt-3 flex items-end justify-between">
              <div
                className={`text-3xl font-extrabold ${
                  stat.color || "text-slate-900"
                }`}
              >
                {typeof stat.value === "number"
                  ? stat.value.toLocaleString()
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
                  {stat.trendUp ? (
                    <ArrowUpOutlined />
                  ) : (
                    <ArrowDownOutlined />
                  )}
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

export default BaseStatsGrid;