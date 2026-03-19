import { Button, Layout, Space, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useMemo, useState } from "react"
import BaseTable from "../../components/base/BaseTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getInventory } from "../../services/inventory"
import {
  approveMedicineRequest,
  getMedicineRequests,
  rejectMedicineRequest,
  type MedicineRequest,
} from "../../services/medicineRequests"

const { Content, Sider } = Layout
const { Text } = Typography

type ApprovalRow = {
  key: string
  requestId: number
  medicineId: number
  medicine: string
  requestedQuantity: number
  currentStock: number
}

export default function MedicineRequestApprovalPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [requests, setRequests] = useState<MedicineRequest[]>([])
  const [stockByMedicine, setStockByMedicine] = useState<Record<number, number>>({})

  const load = async () => {
    try {
      setLoading(true)
      const [requestData, inventoryData] = await Promise.all([getMedicineRequests(), getInventory()])

      setRequests(requestData.filter((request) => request.status === "PENDING"))

      const stockMap = inventoryData.reduce<Record<number, number>>((acc, row) => {
        acc[row.medicineId] = (acc[row.medicineId] ?? 0) + row.totalStock
        return acc
      }, {})
      setStockByMedicine(stockMap)
    } catch {
      messageApi.error("Failed to load approval queue")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const rows = useMemo<ApprovalRow[]>(() => {
    return requests.flatMap((request) =>
      request.items.map((item) => ({
        key: `${request.requestId}-${item.medicineId}`,
        requestId: request.requestId,
        medicineId: item.medicineId,
        medicine: item.medicineName || `Medicine #${item.medicineId}`,
        requestedQuantity: item.quantity,
        currentStock: stockByMedicine[item.medicineId] ?? 0,
      })),
    )
  }, [requests, stockByMedicine])

  const onApprove = async (requestId: number) => {
    try {
      await approveMedicineRequest(requestId)
      messageApi.success(`Request MR-${String(requestId).padStart(4, "0")} approved`)
      await load()
    } catch {
      messageApi.error("Failed to approve request")
    }
  }

  const onReject = async (requestId: number) => {
    try {
      await rejectMedicineRequest(requestId)
      messageApi.success(`Request MR-${String(requestId).padStart(4, "0")} rejected`)
      await load()
    } catch {
      messageApi.error("Failed to reject request")
    }
  }

  const columns: ColumnsType<ApprovalRow> = [
    {
      title: "Request ID",
      dataIndex: "requestId",
      width: 120,
      render: (value: number) => <Text strong>MR-{String(value).padStart(4, "0")}</Text>,
    },
    { title: "Medicine", dataIndex: "medicine", key: "medicine" },
    {
      title: "Requested Quantity",
      dataIndex: "requestedQuantity",
      width: 160,
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: "Current Stock",
      dataIndex: "currentStock",
      width: 140,
      render: (value: number) => value.toLocaleString(),
    },
    {
      title: "Action",
      key: "action",
      width: 180,
      render: (_, record) => (
        <Space>
          <Button size="small" type="primary" onClick={() => onApprove(record.requestId)}>
            Approve
          </Button>
          <Button size="small" danger onClick={() => onReject(record.requestId)}>
            Reject
          </Button>
        </Space>
      ),
    },
  ]

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
          <TopBar title="Request Approval" subtitle="Manager Review" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseTable
            title={() => (
              <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                Pending medicine requests
              </Text>
            )}
            columns={columns}
            dataSource={rows}
            loading={loading}
            cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
          />
        </Content>
      </Layout>
    </Layout>
  )
}
