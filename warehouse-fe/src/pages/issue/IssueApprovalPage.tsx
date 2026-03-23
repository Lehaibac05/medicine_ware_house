import { Button, Input, Layout, Modal, Space, Switch, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useState } from "react"
import BaseTable from "../../components/base/BaseTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { ApiError } from "../../services/api"
import { approveIssueRequest, getIssueRequests, rejectIssueRequest, type IssueRequest } from "../../services/issue"
import IssueStatusTag from "./components/IssueStatusTag"

const { Content, Sider } = Layout
const { Text } = Typography

export default function IssueApprovalPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<IssueRequest[]>([])
  const [allowPartial, setAllowPartial] = useState(true)
  const [rejectingRequestId, setRejectingRequestId] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState("")
  const [rejectSubmitting, setRejectSubmitting] = useState(false)

  const getActionError = (error: unknown, fallback: string) => {
    if (error instanceof ApiError) {
      if (error.status === 403) return "You do not have permission to perform this action"
      if (error.status === 401) return "You need to login again"
      return `${fallback} (${error.status}: ${error.statusText})`
    }
    return fallback
  }

  const loadData = async () => {
    try {
      setLoading(true)
      const data = await getIssueRequests()
      setRows(data.filter((row) => row.status === "PENDING"))
    } catch {
      messageApi.error("Failed to load pending requests")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  const onApprove = async (requestId: number) => {
    try {
      await approveIssueRequest(requestId, { allowPartial })
      messageApi.success("Request approved")
      await loadData()
    } catch (error) {
      messageApi.error(getActionError(error, "Failed to approve request"))
    }
  }

  const onReject = (requestId: number) => {
    setRejectingRequestId(requestId)
    setRejectReason("")
  }

  const submitReject = async () => {
    if (!rejectingRequestId) return

    const trimmedReason = rejectReason.trim()
    if (!trimmedReason) {
      messageApi.warning("Reject reason is required")
      return
    }

    try {
      setRejectSubmitting(true)
      await rejectIssueRequest(rejectingRequestId, { reason: trimmedReason })
      messageApi.success("Request rejected")
      setRejectingRequestId(null)
      setRejectReason("")
      await loadData()
    } catch (error) {
      messageApi.error(getActionError(error, "Failed to reject request"))
    } finally {
      setRejectSubmitting(false)
    }
  }

  const columns: ColumnsType<IssueRequest> = [
    { title: "Request ID", dataIndex: "orderId", width: 110 },
    { title: "Medicine", dataIndex: "medicineName" },
    { title: "Qty", dataIndex: "requestedQuantity", width: 90 },
    {
      title: "Available",
      dataIndex: "availableStockInWarehouse",
      width: 100,
      render: (v?: number) => v ?? 0,
    },
    { title: "Department", dataIndex: "department", width: 140 },
    { title: "Purpose", dataIndex: "purpose", width: 220 },
    {
      title: "Stock Check",
      width: 230,
      render: (_: unknown, record) => {
        const available = record.availableStockInWarehouse ?? 0
        const requested = record.requestedQuantity ?? 0
        if (available >= requested) {
          return <Text type="success">Sufficient stock</Text>
        }
        return <Text type="warning">Insufficient stock ({available}/{requested})</Text>
      },
    },
    { title: "Status", dataIndex: "status", width: 120, render: (v: string) => <IssueStatusTag status={v} /> },
    {
      title: "Action",
      width: 220,
      render: (_: unknown, record) => (
        <Space>
          <Button type="primary" size="small" onClick={() => void onApprove(record.orderId)}>
            Approve
          </Button>
          <Button danger size="small" onClick={() => onReject(record.orderId)}>
            Reject
          </Button>
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
          <TopBar />
        </div>
        <Content className="p-6 pt-[114px] flex flex-col gap-4">
          <div className="rounded-xl bg-white p-4 shadow-[0_12px_28px_rgba(15,23,42,0.06)] flex items-center gap-3">
            <Text>Allow partial approval when stock is insufficient</Text>
            <Switch checked={allowPartial} onChange={setAllowPartial} />
          </div>
          <BaseTable
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Pending issue requests</Text>}
            columns={columns}
            dataSource={rows}
            rowKey="orderId"
            loading={loading}
          />
        </Content>
      </Layout>

      <Modal
        title="Reject Request"
        open={Boolean(rejectingRequestId)}
        onCancel={() => {
          if (rejectSubmitting) return
          setRejectingRequestId(null)
          setRejectReason("")
        }}
        onOk={() => void submitReject()}
        okText="Reject"
        okButtonProps={{ danger: true, loading: rejectSubmitting }}
      >
        <div className="space-y-2">
          <Text className="text-slate-600">Please provide a reason for rejection.</Text>
          <Input.TextArea
            rows={4}
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
            placeholder="Enter reject reason"
            maxLength={500}
            showCount
          />
        </div>
      </Modal>
    </Layout>
  )
}
