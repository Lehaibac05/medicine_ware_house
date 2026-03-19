import { Button, Layout, Typography, message } from "antd"
import { useEffect, useMemo, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import BatchTable from "../../components/erp/BatchTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getInventoryDetail, type InventoryDetailGroup } from "../../services/inventory"

const { Content, Sider } = Layout
const { Text } = Typography

export default function InventoryDetailPage() {
  const navigate = useNavigate()
  const { medicineId } = useParams<{ medicineId: string }>()
  const [messageApi, contextHolder] = message.useMessage()
  const [groups, setGroups] = useState<InventoryDetailGroup[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const id = Number(medicineId)
    if (!id || Number.isNaN(id)) {
      messageApi.error("Invalid medicine id")
      navigate("/inventory", { replace: true })
      return
    }

    const load = async () => {
      setLoading(true)
      try {
        const data = await getInventoryDetail(id)
        setGroups(data)
      } catch {
        messageApi.error("Failed to load inventory detail")
      } finally {
        setLoading(false)
      }
    }

    void load()
  }, [medicineId, messageApi, navigate])

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
    <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="Inventory Detail" subtitle="Batch Traceability" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <div className="rounded-2xl bg-white p-4 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Link to="/inventory">
              <Button>Back to Inventory</Button>
            </Link>
          </div>

          <section className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Medicine</Text>
            <h2 className="mt-1 text-2xl font-bold text-slate-900">{medicineName}</h2>
            <p className="mt-3 text-sm text-slate-600">
              Total stock across warehouses: <strong>{totalStock.toLocaleString()}</strong>
            </p>
          </section>

          <section className="rounded-2xl bg-white p-1 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <BatchTable rows={rows} />
            {loading ? <p className="px-5 pb-4 text-sm text-slate-500">Loading inventory detail...</p> : null}
          </section>
        </Content>
      </Layout>
    </Layout>
  )
}
