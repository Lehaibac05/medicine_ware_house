import type { Alert } from "../../services/alerts"
import StatusBadge from "./StatusBadge"

type AlertTableProps = {
  rows: Alert[]
}

const fmtDate = (value?: string) => {
  if (!value) return "-"
  return new Date(value).toLocaleString("en-GB")
}

export default function AlertTable({ rows }: AlertTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.12em] text-slate-500">
            <tr>
              <th className="px-4 py-3">Alert Type</th>
              <th className="px-4 py-3">Medicine</th>
              <th className="px-4 py-3">Warehouse</th>
              <th className="px-4 py-3">Severity</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Created At</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.alertId} className="border-t border-slate-100">
                <td className="px-4 py-3 font-medium text-slate-900">{row.alertType}</td>
                <td className="px-4 py-3 text-slate-700">{row.medicineName ?? "-"}</td>
                <td className="px-4 py-3 text-slate-700">{row.warehouseName ?? "-"}</td>
                <td className="px-4 py-3"><StatusBadge kind={row.severity} /></td>
                <td className="px-4 py-3"><StatusBadge kind={row.status} /></td>
                <td className="px-4 py-3 text-slate-600">{fmtDate(row.createdAt)}</td>
              </tr>
            ))}
            {rows.length === 0 ? (
              <tr>
                <td className="px-4 py-8 text-center text-slate-500" colSpan={6}>
                  No active alerts
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  )
}
