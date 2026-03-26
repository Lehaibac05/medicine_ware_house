import { Button, Layout, Space, Typography } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useState } from "react"
import BaseTable from "../../components/base/BaseTable"
import { useToast } from "../../hooks/useToast"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { executeIssue, getIssueRequests, type IssueRequest } from "../../services/issue"
import IssueStatusTag from "./components/IssueStatusTag"

const { Content, Sider } = Layout
const { Text } = Typography

export default function IssueExecutionPage() {
  const { toast, contextHolder } = useToast()
  const [loading, setLoading] = useState(false)
  const [executingId, setExecutingId] = useState<number | null>(null)
  const [rows, setRows] = useState<IssueRequest[]>([])

  const loadData = async () => {
    try {
      setLoading(true)
      const data = await getIssueRequests()
      setRows(data.filter((row) => row.status === "APPROVED"))
    } catch (error) {
      toast.error(error, "Không thể tải danh sách yêu cầu đã duyệt")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  const onExecute = async (requestId: number) => {
    try {
      setExecutingId(requestId)
      await executeIssue(requestId)
      toast.success("Thực hiện cấp thuốc thành công")
      await loadData()
    } catch (error) {
      toast.error(error, "Thực hiện cấp thuốc thất bại")
    } finally {
      setExecutingId(null)
    }
  }

  const columns: ColumnsType<IssueRequest> = [
    { title: "Mã yêu cầu", dataIndex: "orderId", width: 100 },
    { title: "Thuốc", dataIndex: "medicineName" },
    { title: "Kho", dataIndex: "warehouseName", width: 140 },
    { title: "SL duyệt", dataIndex: "approvedQuantity", width: 120 },
    { title: "Tồn kho", dataIndex: "availableStockInWarehouse", width: 100, render: (v?: number) => v ?? 0 },
    { title: "Khoa/Phòng", dataIndex: "department", width: 140 },
    { title: "Trạng thái", dataIndex: "status", width: 120, render: (v: string) => <IssueStatusTag status={v} /> },
    {
      title: "Thao tác",
      width: 160,
      render: (_: unknown, record) => (
        <Space>
          <Button type="primary" size="small" loading={executingId === record.orderId} onClick={() => void onExecute(record.orderId)}>
            Thực hiện cấp
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
        <Content className="p-6 pt-[114px]">
          <BaseTable
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Yêu cầu cấp thuốc đã duyệt</Text>}
            columns={columns}
            dataSource={rows}
            rowKey="orderId"
            loading={loading}
          />
        </Content>
      </Layout>
    </Layout>
  )
}
