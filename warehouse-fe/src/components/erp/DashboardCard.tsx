import type { ReactNode } from "react"

type DashboardCardProps = {
  title: string
  value: string | number
  helper?: string
  icon?: ReactNode
}

export default function DashboardCard({ title, value, helper, icon }: DashboardCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{title}</p>
          <p className="mt-3 text-3xl font-bold text-slate-900">{value}</p>
          {helper ? <p className="mt-2 text-sm text-slate-500">{helper}</p> : null}
        </div>
        {icon ? <div className="rounded-xl bg-slate-100 p-2.5 text-slate-700">{icon}</div> : null}
      </div>
    </div>
  )
}
