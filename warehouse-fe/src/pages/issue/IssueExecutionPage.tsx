import { Button, Layout, Space, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useState } from "react"
import BaseTable from "../../components/base/BaseTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { executeIssue, getIssueRequests, type IssueRequest } from "../../services/issue"
import IssueStatusTag from "./components/IssueStatusTag"

const { Content, Sider } = Layout
const { Text } = Typography

export default function IssueExecutionPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [executingId, setExecutingId] = useState<number | null>(null)
  const [rows, setRows] = useState<IssueRequest[]>([])

  const loadData = async () => {
    try {
      setLoading(true)
      const data = await getIssueRequests()
      setRows(data.filter((row) => row.status === "APPROVED"))
    } catch {
      messageApi.error("Failed to load approved requests")
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
      messageApi.success("Issue executed successfully")
      await loadData()
    } catch {
      messageApi.error("Failed to execute issue")
    } finally {
      setExecutingId(null)
    }
  }

  const columns: ColumnsType<IssueRequest> = [
    { title: "Request ID", dataIndex: "orderId", width: 100 },
    { title: "Medicine", dataIndex: "medicineName" },
    { title: "Warehouse", dataIndex: "warehouseName", width: 140 },
    { title: "Approved Qty", dataIndex: "approvedQuantity", width: 120 },
    { title: "Available", dataIndex: "availableStockInWarehouse", width: 100, render: (v?: number) => v ?? 0 },
    { title: "Issued Qty", dataIndex: "issuedQuantity", width: 110, render: (v?: number) => v ?? 0 },
    { title: "Department", dataIndex: "department", width: 140 },
    { title: "Status", dataIndex: "status", width: 120, render: (v: string) => <IssueStatusTag status={v} /> },
    {
      title: "Action",
      width: 160,
      render: (_: unknown, record) => (
        <Space>
          <Button type="primary" size="small" loading={executingId === record.orderId} onClick={() => void onExecute(record.orderId)}>
            Execute Issue
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
          <TopBar title="Issue Execution" subtitle="Issue" />
        </div>
        <Content className="p-6 pt-[114px]">
          <BaseTable
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Approved requests</Text>}
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
