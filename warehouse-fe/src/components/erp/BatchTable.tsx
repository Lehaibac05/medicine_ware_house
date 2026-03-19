type BatchRow = {
  batchId: number
  lotNumber: string
  quantity: number
  manufactureDate?: string
  expiryDate?: string
  warehouseName: string
}

type BatchTableProps = {
  rows: BatchRow[]
}

const fmtDate = (value?: string) => {
  if (!value) return "-"
  return new Date(value).toLocaleDateString("en-GB")
}

export default function BatchTable({ rows }: BatchTableProps) {
  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.12em] text-slate-500">
            <tr>
              <th className="px-4 py-3">Batch Number</th>
              <th className="px-4 py-3">Quantity</th>
              <th className="px-4 py-3">Manufacture Date</th>
              <th className="px-4 py-3">Expiry Date</th>
              <th className="px-4 py-3">Warehouse</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.batchId} className="border-t border-slate-100">
                <td className="px-4 py-3 font-medium text-slate-900">{row.lotNumber}</td>
                <td className="px-4 py-3 text-slate-700">{row.quantity.toLocaleString()}</td>
                <td className="px-4 py-3 text-slate-600">{fmtDate(row.manufactureDate)}</td>
                <td className="px-4 py-3 text-slate-600">{fmtDate(row.expiryDate)}</td>
                <td className="px-4 py-3 text-slate-700">{row.warehouseName}</td>
              </tr>
            ))}
            {rows.length === 0 ? (
              <tr>
                <td className="px-4 py-8 text-center text-slate-500" colSpan={5}>
                  No batch data
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  )
}
