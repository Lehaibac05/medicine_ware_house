import { Card, Empty, Typography } from "antd"

const { Text } = Typography

type ChartPoint = {
  label: string
  value: number
}

type ChartWidgetProps = {
  title: string
  type: "bar" | "pie" | "line"
  data: ChartPoint[]
}

const palette = ["#2563eb", "#f59e0b", "#10b981", "#ef4444", "#7c3aed", "#14b8a6"]

const BarChart = ({ data }: { data: ChartPoint[] }) => {
  const max = Math.max(1, ...data.map((d) => d.value))
  return (
    <div className="grid grid-cols-1 gap-3">
      {data.map((item, index) => (
        <div key={item.label}>
          <div className="mb-1 flex items-center justify-between text-xs text-slate-500">
            <span>{item.label}</span>
            <span>{item.value}</span>
          </div>
          <div className="h-2 rounded-full bg-slate-100">
            <div
              className="h-2 rounded-full transition-all"
              style={{
                width: `${(item.value / max) * 100}%`,
                backgroundColor: palette[index % palette.length],
              }}
            />
          </div>
        </div>
      ))}
    </div>
  )
}

const PieChart = ({ data }: { data: ChartPoint[] }) => {
  const total = data.reduce((sum, item) => sum + item.value, 0)
  if (total <= 0) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No data" />

  let accumulated = 0
  const segments = data
    .map((item, index) => {
      const start = (accumulated / total) * 360
      accumulated += item.value
      const end = (accumulated / total) * 360
      return {
        ...item,
        color: palette[index % palette.length],
        start,
        end,
      }
    })

  return (
    <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
      <div
        className="h-44 w-44 rounded-full"
        style={{
          background: `conic-gradient(${segments
            .map((s) => `${s.color} ${s.start}deg ${s.end}deg`)
            .join(", ")})`,
        }}
      />
      <div className="grid gap-2">
        {segments.map((item) => (
          <div key={item.label} className="flex items-center gap-2 text-sm">
            <span className="inline-block h-3 w-3 rounded-full" style={{ backgroundColor: item.color }} />
            <span className="text-slate-600">{item.label}</span>
            <span className="font-semibold text-slate-900">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

const LineChart = ({ data }: { data: ChartPoint[] }) => {
  const max = Math.max(1, ...data.map((item) => item.value))
  const points = data
    .map((item, index) => {
      const x = data.length === 1 ? 50 : (index / (data.length - 1)) * 100
      const y = 100 - (item.value / max) * 100
      return `${x},${y}`
    })
    .join(" ")

  return (
    <div className="space-y-2">
      <svg viewBox="0 0 100 100" className="h-44 w-full rounded-xl bg-slate-50 p-2">
        <polyline fill="none" stroke="#2563eb" strokeWidth="2" points={points} />
      </svg>
      <div className="grid grid-cols-3 gap-2 text-xs text-slate-500">
        {data.map((item) => (
          <div key={item.label} className="rounded-lg bg-slate-50 p-2 text-center">
            <div>{item.label}</div>
            <div className="font-semibold text-slate-800">{item.value}</div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default function ChartWidget({ title, type, data }: ChartWidgetProps) {
  return (
    <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">{title}</Text>
      <div className="mt-3">
        {data.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No data" />}
        {data.length > 0 && type === "bar" && <BarChart data={data} />}
        {data.length > 0 && type === "pie" && <PieChart data={data} />}
        {data.length > 0 && type === "line" && <LineChart data={data} />}
      </div>
    </Card>
  )
}
