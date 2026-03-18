import { Button, Layout, Space, Tag, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useMemo, useState } from "react"
import { Link } from "react-router-dom"
import BaseTable from "../../components/base/BaseTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import {
  approveMedicineRequest,
  getMedicineRequests,
  getMyMedicineRequests,
  rejectMedicineRequest,
  type MedicineRequest,
} from "../../services/medicineRequests"
import { hasAnyRole } from "../../utils/auth"

const { Content, Sider } = Layout
const { Text } = Typography

type RequestRow = {
  key: string
  requestId: number
  medicine: string
  quantity: number
  requestedBy: string
  createdDate: string
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

export default function MedicineRequestsListPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [requests, setRequests] = useState<MedicineRequest[]>([])
  const [actingRequestId, setActingRequestId] = useState<number | null>(null)
  const [actingType, setActingType] = useState<"approve" | "reject" | null>(null)
  const isManagerView = hasAnyRole(["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"])

  const loadRequests = async () => {
    try {
      setLoading(true)
      const data = isManagerView
        ? await getMedicineRequests()
        : await getMyMedicineRequests()
      setRequests(data)
    } catch {
      messageApi.error("Failed to load medicine requests")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadRequests()
  }, [isManagerView, messageApi])

  const onApprove = async (requestId: number) => {
    try {
      setActingRequestId(requestId)
      setActingType("approve")
      await approveMedicineRequest(requestId)
      messageApi.success("Request approved")
      await loadRequests()
    } catch {
      messageApi.error("Failed to approve request")
    } finally {
      setActingRequestId(null)
      setActingType(null)
    }
  }

  const onReject = async (requestId: number) => {
    try {
      setActingRequestId(requestId)
      setActingType("reject")
      await rejectMedicineRequest(requestId)
      messageApi.success("Request rejected")
      await loadRequests()
    } catch {
      messageApi.error("Failed to reject request")
    } finally {
      setActingRequestId(null)
      setActingType(null)
    }
  }

  const rows = useMemo<RequestRow[]>(
    () =>
      requests.map((request) => {
        const first = request.items[0]
        const medLabel = first?.medicineName
          ? request.items.length > 1
            ? `${first.medicineName} +${request.items.length - 1}`
            : first.medicineName
          : `Medicine #${first?.medicineId ?? "-"}`

        const quantity = request.items.reduce((acc, item) => acc + item.quantity, 0)

        return {
          key: String(request.requestId),
          requestId: request.requestId,
          medicine: medLabel,
          quantity,
          requestedBy: request.requestedBy,
          createdDate: new Date(request.createdDate).toLocaleDateString("en-GB"),
          status: normalizeStatus(request.status),
        }
      }),
    [requests],
  )

  const columns: ColumnsType<RequestRow> = [
    {
      title: "Request ID",
      dataIndex: "requestId",
      render: (value: number) => <Text strong>MR-{String(value).padStart(4, "0")}</Text>,
      width: 130,
    },
    { title: "Medicine", dataIndex: "medicine", key: "medicine" },
    {
      title: "Quantity",
      dataIndex: "quantity",
      key: "quantity",
      render: (value: number) => value.toLocaleString(),
      width: 120,
    },
    { title: "Requested By", dataIndex: "requestedBy", key: "requestedBy", width: 180 },
    { title: "Created Date", dataIndex: "createdDate", key: "createdDate", width: 140 },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (value: RequestRow["status"]) => statusTag(value),
      width: 120,
    },
    {
      title: "Action",
      key: "action",
      width: 260,
      render: (_, record) => (
        <Space>
          {isManagerView ? (
            <>
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
            </>
          ) : null}
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
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="Medicine Requests" subtitle="Procurement" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseTable
            title={() => (
              <div className="flex items-center justify-between">
                <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                  Request list
                </Text>
                <Space>
                  {isManagerView ? (
                    <Link to="/medicine-requests/approval">
                      <Button>Approval Queue</Button>
                    </Link>
                  ) : null}
                  <Link to="/medicine-requests/create">
                    <Button type="primary">Create Request</Button>
                  </Link>
                </Space>
              </div>
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
