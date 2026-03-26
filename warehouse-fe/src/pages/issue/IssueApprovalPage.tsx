import { Button, Input, Layout, Modal, Space, Switch, Typography } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useState } from "react"
import BaseTable from "../../components/base/BaseTable"
import { useToast } from "../../hooks/useToast"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { approveIssueRequest, getIssueRequests, rejectIssueRequest, type IssueRequest } from "../../services/issue"
import IssueStatusTag from "./components/IssueStatusTag"

const { Content, Sider } = Layout
const { Text } = Typography

export default function IssueApprovalPage() {
  const { toast, contextHolder } = useToast()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<IssueRequest[]>([])
  const [allowPartial, setAllowPartial] = useState(true)
  const [rejectingRequestId, setRejectingRequestId] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState("")
  const [rejectSubmitting, setRejectSubmitting] = useState(false)

  const loadData = async () => {
    try {
      setLoading(true)
      const data = await getIssueRequests()
      setRows(data.filter((row) => row.status === "PENDING"))
    } catch (error) {
      toast.error(error, "Không thể tải danh sách yêu cầu chờ duyệt")
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
      toast.success("Duyệt yêu cầu thành công")
      await loadData()
    } catch (error) {
      toast.error(error, "Duyệt yêu cầu thất bại")
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
      toast.warning("Vui lòng nhập lý do từ chối")
      return
    }

    try {
      setRejectSubmitting(true)
      await rejectIssueRequest(rejectingRequestId, { reason: trimmedReason })
      toast.success("Từ chối yêu cầu thành công")
      setRejectingRequestId(null)
      setRejectReason("")
      await loadData()
    } catch (error) {
      toast.error(error, "Từ chối yêu cầu thất bại")
    } finally {
      setRejectSubmitting(false)
    }
  }

  const columns: ColumnsType<IssueRequest> = [
    { title: "Mã yêu cầu", dataIndex: "orderId", width: 110 },
    { title: "Thuốc", dataIndex: "medicineName" },
    { title: "Số lượng", dataIndex: "requestedQuantity", width: 90 },
    {
      title: "Tồn kho",
      dataIndex: "availableStockInWarehouse",
      width: 100,
      render: (v?: number) => v ?? 0,
    },
    { title: "Khoa/Phòng", dataIndex: "department", width: 140 },
    { title: "Mục đích", dataIndex: "purpose", width: 220 },
    {
      title: "Đối chiếu tồn kho",
      width: 230,
      render: (_: unknown, record) => {
        const available = record.availableStockInWarehouse ?? 0
        const requested = record.requestedQuantity ?? 0
        if (available >= requested) {
          return <Text type="success">Đủ tồn kho</Text>
        }
        return <Text type="warning">Thiếu tồn kho ({available}/{requested})</Text>
      },
    },
    { title: "Trạng thái", dataIndex: "status", width: 120, render: (v: string) => <IssueStatusTag status={v} /> },
    {
      title: "Thao tác",
      width: 220,
      render: (_: unknown, record) => (
        <Space>
          <Button type="primary" size="small" onClick={() => void onApprove(record.orderId)}>
            Duyệt
          </Button>
          <Button danger size="small" onClick={() => onReject(record.orderId)}>
            Từ chối
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
            <Text>Cho phép duyệt một phần khi tồn kho không đủ</Text>
            <Switch checked={allowPartial} onChange={setAllowPartial} />
          </div>
          <BaseTable
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Yêu cầu cấp thuốc chờ duyệt</Text>}
            columns={columns}
            dataSource={rows}
            rowKey="orderId"
            loading={loading}
          />
        </Content>
      </Layout>

      <Modal
        title="Từ chối yêu cầu"
        open={Boolean(rejectingRequestId)}
        onCancel={() => {
          if (rejectSubmitting) return
          setRejectingRequestId(null)
          setRejectReason("")
        }}
        onOk={() => void submitReject()}
        okText="Từ chối"
        cancelText="Hủy"
        okButtonProps={{ danger: true, loading: rejectSubmitting }}
      >
        <div className="space-y-2">
          <Text className="text-slate-600">Vui lòng nhập lý do từ chối yêu cầu này.</Text>
          <Input.TextArea
            rows={4}
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
            placeholder="Nhập lý do từ chối"
            maxLength={500}
            showCount
          />
        </div>
      </Modal>
    </Layout>
  )
}
