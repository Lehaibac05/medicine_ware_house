import { Card, Typography } from "antd"

const { Text } = Typography

type SummaryCardProps = {
  label: string
  value: string | number
  note?: string
  tone?: "default" | "danger" | "warning" | "success"
}

const toneMap = {
  default: "text-slate-900",
  danger: "text-red-600",
  warning: "text-amber-600",
  success: "text-emerald-600",
}

export default function SummaryCard({ label, value, note, tone = "default" }: SummaryCardProps) {
  return (
    <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">{label}</Text>
      <div className={`mt-2 text-3xl font-bold ${toneMap[tone]}`}>{value}</div>
      {note && <Text className="text-[13px] text-slate-500">{note}</Text>}
    </Card>
  )
}
