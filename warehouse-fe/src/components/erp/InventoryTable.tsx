import { Link } from "react-router-dom"
import StatusBadge from "./StatusBadge"
import type { InventoryRow } from "../../services/inventory"

type InventoryTableProps = {
  rows: InventoryRow[]
}

const fmtDate = (value?: string) => {
  if (!value) return "-"
  return new Date(value).toLocaleDateString("en-GB")
}

export default function InventoryTable({ rows }: InventoryTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.12em] text-slate-500">
            <tr>
              <th className="px-4 py-3">Medicine Name</th>
              <th className="px-4 py-3">Warehouse</th>
              <th className="px-4 py-3">Total Stock</th>
              <th className="px-4 py-3">Batch Count</th>
              <th className="px-4 py-3">Nearest Expiry</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Action</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${row.medicineId}-${row.warehouseId}`} className="border-t border-slate-100">
                <td className="px-4 py-3 font-medium text-slate-900">{row.medicineName}</td>
                <td className="px-4 py-3 text-slate-700">{row.warehouseName}</td>
                <td className="px-4 py-3 text-slate-700">{row.totalStock.toLocaleString()}</td>
                <td className="px-4 py-3 text-slate-700">{row.batchCount}</td>
                <td className="px-4 py-3 text-slate-600">{fmtDate(row.nearestExpiryDate)}</td>
                <td className="px-4 py-3"><StatusBadge kind={row.status} /></td>
                <td className="px-4 py-3">
                  <Link
                    to={`/inventory/${row.medicineId}`}
                    className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                  >
                    View Detail
                  </Link>
                </td>
              </tr>
            ))}
            {rows.length === 0 ? (
              <tr>
                <td className="px-4 py-8 text-center text-slate-500" colSpan={7}>
                  No inventory data
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  )
}
