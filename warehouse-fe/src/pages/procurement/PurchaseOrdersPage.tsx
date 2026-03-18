import { Button, Input, Layout, Select, Space, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useMemo, useState } from "react"
import { Link } from "react-router-dom"
import BaseTable from "../../components/base/BaseTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getPurchaseOrders, type PurchaseOrder } from "../../services/purchaseOrders"
import StatusTag from "../../components/common/StatusTag"

const { Content, Sider } = Layout
const { Text } = Typography

type PurchaseOrderRow = {
  key: string
  purchaseOrderId: number
  poCode: string
  supplier: string
  warehouse: string
  totalAmount: number
  status: string
}

const statusTag = (status: string) => {
  return <StatusTag domain="purchaseOrder" status={status} />
}

export default function PurchaseOrdersPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [orders, setOrders] = useState<PurchaseOrder[]>([])
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        const data = await getPurchaseOrders()
        setOrders(data)
      } catch {
        messageApi.error("Failed to load purchase orders")
      } finally {
        setLoading(false)
      }
    }

    void load()
  }, [messageApi])

  const rows = useMemo<PurchaseOrderRow[]>(() => {
    return orders
      .filter((order) => {
        if (!search.trim()) return true
        const keyword = search.trim().toLowerCase()
        return (
          order.orderCode?.toLowerCase().includes(keyword) ||
          order.supplier?.supplierName?.toLowerCase().includes(keyword)
        )
      })
      .filter((order) => {
        if (statusFilter === "all") return true
        return order.status.toUpperCase() === statusFilter
      })
      .map((order) => ({
        key: String(order.purchaseOrderId),
        purchaseOrderId: order.purchaseOrderId,
        poCode: order.orderCode,
        supplier: order.supplier?.supplierName || "-",
        warehouse: order.warehouse?.warehouseName || "-",
        totalAmount: Number(order.totalAmount || 0),
        status: order.status,
      }))
  }, [orders, search, statusFilter])

  const columns: ColumnsType<PurchaseOrderRow> = [
    {
      title: "PO Code",
      dataIndex: "poCode",
      key: "poCode",
      render: (value: string) => <Text strong>{value}</Text>,
      width: 140,
    },
    { title: "Supplier", dataIndex: "supplier", key: "supplier" },
    { title: "Warehouse", dataIndex: "warehouse", key: "warehouse" },
    {
      title: "Total Amount",
      dataIndex: "totalAmount",
      key: "totalAmount",
      render: (value: number) => value.toLocaleString(),
      width: 150,
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (value: string) => statusTag(value),
      width: 130,
    },
    {
      title: "Action",
      key: "action",
      width: 120,
      render: (_, record) => (
        <Link to={`/purchase-orders/${record.purchaseOrderId}`}>
          <Button size="small">View</Button>
        </Link>
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
          <TopBar title="Purchase Orders" subtitle="Procurement" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <div className="grid gap-3 rounded-2xl bg-white p-4 shadow-[0_12px_28px_rgba(15,23,42,0.06)] md:grid-cols-3">
            <Input
              placeholder="Search PO code or supplier"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Select
              value={statusFilter}
              onChange={setStatusFilter}
              options={[
                { value: "all", label: "All status" },
                { value: "PENDING", label: "PENDING" },
                { value: "CONFIRMED", label: "CONFIRMED" },
                { value: "SHIPPING", label: "SHIPPED" },
                { value: "RECEIVED", label: "RECEIVED" },
                { value: "APPROVED", label: "APPROVED" },
              ]}
            />
            <Space className="justify-end">
              <Link to="/purchase-orders/create">
                <Button>Create PO</Button>
              </Link>
            </Space>
          </div>

          <BaseTable
            title={() => (
              <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                Purchase order list
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
