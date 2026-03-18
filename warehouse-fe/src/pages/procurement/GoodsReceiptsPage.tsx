import { Button, Layout, Space, Table, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { AxiosError } from "axios"
import { useNavigate } from "react-router-dom"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { useApproveGoodsReceiptMutation, useGoodsReceiptsQuery } from "../../hooks/useWorkflow"
import { clearAuthToken } from "../../utils/auth"
import StatusTag from "../../components/common/StatusTag"

const { Content, Sider } = Layout
const { Text } = Typography

type ReceiptRow = {
  key: string
  receiptId: number
  receiptCode: string
  purchaseOrder: string
  receivedBy: string
  status: string
}

const normalizeReceiptStatus = (status?: string) =>
  status?.trim().toUpperCase().replace(/[\s-]+/g, "_") ?? ""

export default function GoodsReceiptsPage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const { data: receipts = [], isLoading } = useGoodsReceiptsQuery()
  const approveMutation = useApproveGoodsReceiptMutation()

  const rows: ReceiptRow[] = receipts.map((receipt) => ({
    key: String(receipt.receiptId),
    receiptId: receipt.receiptId,
    receiptCode: receipt.receiptCode,
    purchaseOrder: receipt.purchaseOrder?.orderCode || "-",
    receivedBy: receipt.receivedBy?.fullName || receipt.receivedBy?.username || "-",
    status: normalizeReceiptStatus(receipt.status),
  }))

  const onApprove = async (row: ReceiptRow) => {
    try {
      await approveMutation.mutateAsync({ id: row.receiptId })
      messageApi.success("Receipt approved")
    } catch (error) {
      if (error instanceof AxiosError) {
        if (error.response?.status === 401) {
          clearAuthToken()
          messageApi.error("Session expired. Please login again")
          navigate("/login", { replace: true })
          return
        }

        if (error.response?.status === 403) {
          messageApi.error("You do not have permission to approve this receipt")
          return
        }

        const responseData = error.response?.data
        const serverMessage = typeof responseData === "string"
          ? responseData
          : responseData?.message
        const statusText = error.response?.status ? `HTTP ${error.response.status}` : ""
        messageApi.error(serverMessage || statusText || "Failed to approve receipt")
        return
      }
      messageApi.error("Failed to approve receipt")
    }
  }

  const columns: ColumnsType<ReceiptRow> = [
    { title: "Receipt Code", dataIndex: "receiptCode", width: 160 },
    { title: "Purchase Order", dataIndex: "purchaseOrder", width: 160 },
    { title: "Received By", dataIndex: "receivedBy" },
    {
      title: "Status",
      dataIndex: "status",
      width: 200,
      render: (value: string) => <StatusTag domain="goodsReceipt" status={normalizeReceiptStatus(value)} />,
    },
    {
      title: "Action",
      width: 180,
      render: (_: unknown, record: ReceiptRow) => (
        <Space>
          <Button
            size="small"
            onClick={() => void onApprove(record)}
            loading={approveMutation.isPending && approveMutation.variables?.id === record.receiptId}
          >
            Approve Receipt
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
          <TopBar title="Goods Receipts" subtitle="Manager Approval" />
        </div>

        <Content className="p-6 pt-[114px]">
          <Table
            rowKey="key"
            loading={isLoading}
            columns={columns}
            dataSource={rows}
            pagination={{ pageSize: 10, showSizeChanger: true }}
            className="rounded-2xl bg-white p-2 shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Goods receipt approvals</Text>}
          />
        </Content>
      </Layout>
    </Layout>
  )
}
