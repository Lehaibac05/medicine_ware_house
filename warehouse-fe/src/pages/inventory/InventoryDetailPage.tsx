import { useEffect, useMemo, useState } from "react"
import { useParams } from "react-router-dom"
import BatchTable from "../../components/erp/BatchTable"
import ERPLayout from "../../layouts/ERPLayout"
import { getInventoryDetail, type InventoryDetailGroup } from "../../services/inventory"

export default function InventoryDetailPage() {
  const { medicineId } = useParams<{ medicineId: string }>()
  const [groups, setGroups] = useState<InventoryDetailGroup[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const id = Number(medicineId)
    if (!id || Number.isNaN(id)) return

    const load = async () => {
      setLoading(true)
      try {
        const data = await getInventoryDetail(id)
        setGroups(data)
      } finally {
        setLoading(false)
      }
    }

    void load()
  }, [medicineId])

  const medicineName = groups[0]?.medicineName ?? "Inventory Detail"
  const totalStock = useMemo(
    () => groups.reduce((acc, group) => acc + group.totalStock, 0),
    [groups],
  )

  const rows = useMemo(
    () =>
      groups.flatMap((group) =>
        group.batches.map((batch) => ({
          batchId: batch.batchId,
          lotNumber: batch.lotNumber,
          quantity: batch.quantity,
          manufactureDate: batch.manufactureDate,
          expiryDate: batch.expiryDate,
          warehouseName: group.warehouseName,
        })),
      ),
    [groups],
  )

  return (
    <ERPLayout title="Inventory Detail" subtitle="Batch Traceability">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Medicine</p>
        <h2 className="mt-1 text-2xl font-bold text-slate-900">{medicineName}</h2>
        <p className="mt-3 text-sm text-slate-600">Total stock across warehouses: <strong>{totalStock.toLocaleString()}</strong></p>
      </section>

      <section className="mt-5">
        <BatchTable rows={rows} />
        {loading ? <p className="mt-3 text-sm text-slate-500">Loading inventory detail...</p> : null}
      </section>
    </ERPLayout>
  )
}
