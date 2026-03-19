import { Button, Input, Layout, Space, Table, Tag, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useMemo, useState } from "react"
import { Link } from "react-router-dom"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { ApiError } from "../../services/api"
import {
  useApproveRequestMutation,
  useInventoryQuery,
  useRejectRequestMutation,
  useRequestsQuery,
} from "../../hooks/useWorkflow"

const { Content, Sider } = Layout
const { Text } = Typography

type RequestRow = {
  key: string
  requestId: number
  medicine: string
  requestedQuantity: number
  currentStock: number
  requiredDate: string
  status: "PENDING" | "APPROVED" | "REJECTED"
}

const statusTag = (status: RequestRow["status"]) => {
  if (status === "APPROVED") return <Tag color="green">APPROVED</Tag>
  if (status === "REJECTED") return <Tag color="red">REJECTED</Tag>
  return <Tag color="gold">PENDING</Tag>
}

const normalizeStatus = (status?: string): RequestRow["status"] => {
  const normalized = status?.trim().toUpperCase()
  if (normalized === "APPROVED") return "APPROVED"
  if (normalized === "REJECTED") return "REJECTED"
  return "PENDING"
}

export default function RequestsManagementPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [search, setSearch] = useState("")
  const [actingRequestId, setActingRequestId] = useState<number | null>(null)
  const [actingType, setActingType] = useState<"approve" | "reject" | null>(null)

  const { data: requests = [], isLoading } = useRequestsQuery()
  const { data: inventory = [], isLoading: isInventoryLoading } = useInventoryQuery()
  const approveMutation = useApproveRequestMutation()
  const rejectMutation = useRejectRequestMutation()

  const stockByMedicineWarehouse = useMemo(() => {
    return inventory.reduce<Record<string, number>>((acc, item) => {
      acc[`${item.medicineId}-${item.warehouseId}`] = item.totalStock
      return acc
    }, {})
  }, [inventory])

  const rows = useMemo<RequestRow[]>(() => {
    return requests
      .map((request) => {
        const quantity = request.items.reduce((sum, item) => sum + item.quantity, 0)
        const firstItem = request.items[0]
        const firstMedicineName = firstItem?.medicineName || `Medicine #${firstItem?.medicineId || "-"}`
        const stockKey = `${firstItem?.medicineId || ""}-${request.warehouseId}`

        return {
          key: String(request.requestId),
          requestId: request.requestId,
          medicine: firstMedicineName,
          requestedQuantity: quantity,
          currentStock: stockByMedicineWarehouse[stockKey] ?? 0,
          requiredDate: request.requiredDate || "-",
          status: normalizeStatus(request.status),
        }
      })
      .filter((row) => {
        if (!search.trim()) return true
        const keyword = search.trim().toLowerCase()
        return (
          String(row.requestId).includes(keyword) ||
          row.medicine.toLowerCase().includes(keyword) ||
          row.status.toLowerCase().includes(keyword)
        )
      })
  }, [requests, search, stockByMedicineWarehouse])

  const getActionErrorMessage = (error: unknown, fallback: string) => {
    if (error instanceof ApiError && error.status === 403) {
      return "You do not have permission to approve/reject requests"
    }
    return fallback
  }

  const onApprove = async (requestId: number) => {
    try {
      setActingRequestId(requestId)
      setActingType("approve")
      await approveMutation.mutateAsync(requestId)
      messageApi.success("Request approved")
    } catch (error) {
      messageApi.error(getActionErrorMessage(error, "Failed to approve request"))
    } finally {
      setActingRequestId(null)
      setActingType(null)
    }
  }

  const onReject = async (requestId: number) => {
    try {
      setActingRequestId(requestId)
      setActingType("reject")
      await rejectMutation.mutateAsync(requestId)
      messageApi.success("Request rejected")
    } catch (error) {
      messageApi.error(getActionErrorMessage(error, "Failed to reject request"))
    } finally {
      setActingRequestId(null)
      setActingType(null)
    }
  }

  const columns: ColumnsType<RequestRow> = [
    { title: "Medicine", dataIndex: "medicine" },
    { title: "Requested Quantity", dataIndex: "requestedQuantity", width: 160 },
    { title: "Current Stock", dataIndex: "currentStock", width: 140 },
    { title: "Required Date", dataIndex: "requiredDate", width: 140 },
    {
      title: "Status",
      dataIndex: "status",
      width: 130,
      render: (value: RequestRow["status"]) => statusTag(value),
    },
    {
      title: "Actions",
      width: 250,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            onClick={() => void onApprove(record.requestId)}
            loading={actingRequestId === record.requestId && actingType === "approve"}
          >
            Approve
          </Button>
          <Button
            size="small"
            danger
            onClick={() => void onReject(record.requestId)}
            loading={actingRequestId === record.requestId && actingType === "reject"}
          >
            Reject
          </Button>
          {record.status === "APPROVED" ? (
            <Link to={`/purchase-orders/create?requestId=${record.requestId}`}>
              <Button size="small" type="primary">
                Create PO
              </Button>
            </Link>
          ) : null}
        </Space>
      ),
    },
  ]

  return (
    <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider width={260} className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen">
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="Medicine Requests" subtitle="Manager Approval" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <div className="rounded-2xl bg-white p-4 shadow-[0_12px_28px_rgba(15,23,42,0.06)] flex items-center justify-between">
            <Input.Search
              className="max-w-[360px]"
              placeholder="Search request"
              allowClear
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Link to="/requests/new">
              <Button type="primary">Create Request</Button>
            </Link>
          </div>

          <Table
            rowKey="key"
            loading={isLoading || isInventoryLoading}
            columns={columns}
            dataSource={rows}
            pagination={{ pageSize: 10, showSizeChanger: true }}
            className="rounded-2xl bg-white p-2 shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Request management</Text>}
          />
        </Content>
      </Layout>
    </Layout>
  )
}
