import { Button, Layout, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useState } from "react"
import { Link } from "react-router-dom"
import BaseTable from "../../components/base/BaseTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getMyIssueRequests, type IssueRequest } from "../../services/issue"
import IssueStatusTag from "./components/IssueStatusTag"

const { Content, Sider } = Layout
const { Text } = Typography

export default function IssueRequestListPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<IssueRequest[]>([])

  const loadData = async () => {
    try {
      setLoading(true)
      setRows(await getMyIssueRequests())
    } catch {
      messageApi.error("Failed to load issue requests")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  const columns: ColumnsType<IssueRequest> = [
    { title: "Request ID", dataIndex: "orderId", width: 110 },
    { title: "Medicine", dataIndex: "medicineName" },
    { title: "Requested", dataIndex: "requestedQuantity", width: 100 },
    { title: "Approved", dataIndex: "approvedQuantity", width: 100, render: (v?: number) => v ?? "-" },
    { title: "Department", dataIndex: "department", width: 140 },
    { title: "Needed Date", dataIndex: "neededDate", width: 170, render: (v?: string) => (v ? new Date(v).toLocaleString() : "-") },
    { title: "Status", dataIndex: "status", width: 120, render: (v: string) => <IssueStatusTag status={v} /> },
    {
      title: "Reject Reason",
      dataIndex: "rejectionReason",
      width: 260,
      render: (value?: string, record?: IssueRequest) => {
        if (record?.status !== "REJECTED") return "-"
        return value?.trim() ? value : "No reason provided"
      },
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
          <TopBar title="My Issue Requests" subtitle="Issue" />
        </div>
        <Content className="p-6 pt-[114px]">
          <BaseTable
            title={() => (
              <div className="flex items-center justify-between">
                <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Issue requests</Text>
                <Link to="/issue-request/create">
                  <Button type="primary">Create Request</Button>
                </Link>
              </div>
            )}
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
