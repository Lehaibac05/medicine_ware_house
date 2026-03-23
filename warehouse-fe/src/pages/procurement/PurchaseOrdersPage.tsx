import {
  Button,
  Input,
  Select,
  Space,
  Typography,
  message,
} from "antd"
import type { ColumnsType } from "antd/es/table"
import { useEffect, useMemo, useState } from "react"
import { Link } from "react-router-dom"
import BaseTable from "../../components/base/BaseTable"
import {
  getPurchaseOrders,
  type PurchaseOrder,
} from "../../services/purchaseOrders"
import StatusTag from "../../components/common/StatusTag"
import MainLayout from "../../layouts/MainLayout"
import { apiFetch } from "../../services/api"
import BaseFilterCard from "../../components/base/BaseFilterCard"
import { getUserRoles } from "../../utils/auth"

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

type Supplier = {
  supplierId: number
  supplierName: string
}

type Warehouse = {
  warehouseId: number
  name: string
}

export default function PurchaseOrdersPage() {
  const [messageApi, contextHolder] = message.useMessage()

  const [loading, setLoading] = useState(false)
  const [orders, setOrders] = useState<PurchaseOrder[]>([])

  const [search, setSearch] = useState("")

  const [filters, setFilters] = useState({
    status: "all",
    supplierId: undefined as number | undefined,
    warehouseId: undefined as number | undefined,
  })
  const [appliedFilters, setAppliedFilters] = useState(filters)

  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const roles = getUserRoles()

  const isAdmin = roles.includes("ROLE_ADMIN")
  const isManager = roles.includes("ROLE_WAREHOUSE_MANAGER")

  const canCreatePurchaseOrder = isAdmin || isManager

  // ================= LOAD FILTER DATA =================
  useEffect(() => {
    const loadFilterData = async () => {
      try {
        const [supRes, wh] = await Promise.all([
          apiFetch<any>("/suppliers"),
          apiFetch<Warehouse[]>("/warehouses"),
        ])

        setSuppliers(supRes.content)
        setWarehouses(wh)
      } catch {
        messageApi.error("Lỗi tải dữ liệu filter")
      }
    }

    void loadFilterData()
  }, [messageApi])

  // ================= LOAD PURCHASE ORDERS =================
  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)

        const data = await getPurchaseOrders({
          status: appliedFilters.status,
          supplierId: appliedFilters.supplierId,
          warehouseId: appliedFilters.warehouseId,
          page: pagination.current - 1,
          size: pagination.pageSize,
        })

        setOrders(data.content)
        setPagination((prev) => ({
          ...prev,
          total: data.totalElements,
        }))
      } catch {
        messageApi.error("Lỗi khi tải dữ liệu đơn mua hàng")
      } finally {
        setLoading(false)
      }
    }

    void load()
  }, [appliedFilters, pagination.current, pagination.pageSize, messageApi])

  // Reset to page 1 when filters change
  useEffect(() => {
    setPagination((prev) => ({
      ...prev,
      current: 1,
    }))
  }, [appliedFilters])

  // ================= TABLE DATA =================
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
      .map((order) => ({
        key: String(order.purchaseOrderId),
        purchaseOrderId: order.purchaseOrderId,
        poCode: order.orderCode,
        supplier: order.supplier?.supplierName || "-",
        warehouse: order.warehouse?.warehouseName || "-",
        totalAmount: Number(order.totalAmount || 0),
        status: order.status,
      }))
  }, [orders, search])

  const columns: ColumnsType<PurchaseOrderRow> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "poCode",
      render: (value: string) => <Text strong>{value}</Text>,
      width: 140,
    },
    { title: "Nhà cung cấp", dataIndex: "supplier" },
    { title: "Kho", dataIndex: "warehouse" },
    {
      title: "Tổng tiền",
      dataIndex: "totalAmount",
      render: (value: number) => value.toLocaleString(),
      width: 150,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      render: (value: string) => (
        <StatusTag domain="purchaseOrder" status={value} />
      ),
      width: 130,
    },
    {
      title: "Hành động",
      width: 120,
      render: (_, record) => (
        <Link to={`/purchase-orders/${record.purchaseOrderId}`}>
          <Button size="small">Xem</Button>
        </Link>
      ),
    },
  ]

  return (
    <MainLayout>
      {contextHolder}

      <BaseFilterCard
        actions={
          <div className="flex gap-2">
            <Button
              className="h-[40px] flex-1"
              onClick={() =>
                setFilters({
                  status: "all",
                  supplierId: undefined,
                  warehouseId: undefined,
                })
              }
            >
              Reset
            </Button>

            <Button
              type="primary"
              className="h-[40px] flex-1"
              onClick={() => {
                setPagination((prev) => ({
                  ...prev,
                  current: 1,
                }))
                setAppliedFilters(filters)
              }}
            >
              Áp dụng
            </Button>
          </div>
        }
      >
        {/* STATUS */}
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select
            value={filters.status}
            onChange={(value) =>
              setFilters((prev) => ({ ...prev, status: value }))
            }
            options={[
              { value: "all", label: "Tất cả trạng thái" },
              { value: "PENDING", label: "Chờ xử lý" },
              { value: "CONFIRMED", label: "Đã xác nhận" },
              { value: "SHIPPING", label: "Đang giao hàng" },
              { value: "RECEIVED", label: "Đã nhận hàng" },
              { value: "APPROVED", label: "Đã duyệt" },
            ]}
          />
        </div>

        {/* SUPPLIER */}
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Nhà cung cấp</Text>
          <Select
            placeholder="Chọn nhà cung cấp"
            allowClear
            value={filters.supplierId}
            onChange={(value) =>
              setFilters((prev) => ({ ...prev, supplierId: value }))
            }
            options={suppliers.map((s) => ({
              value: s.supplierId,
              label: s.supplierName,
            }))}
          />
        </div>

        {/* WAREHOUSE */}
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Kho</Text>
          <Select
            placeholder="Chọn kho"
            allowClear
            value={filters.warehouseId}
            onChange={(value) =>
              setFilters((prev) => ({ ...prev, warehouseId: value }))
            }
            options={warehouses.map((w) => ({
              value: w.warehouseId,
              label: w.name,
            }))}
          />
        </div>
      </BaseFilterCard>

      {/* ================= TABLE ================= */}
      <BaseTable
        title={() => (
          <div className="flex items-center justify-between">
            <Text className="text-[11px] uppercase text-slate-400">
              Danh sách đơn mua hàng
            </Text>

            <Space>
              <Input.Search
                className="w-[260px]"
                placeholder="Tìm kiếm..."
                allowClear
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />

              {canCreatePurchaseOrder && (
                <Link to="/purchase-orders/create">
                  <Button type="primary">Tạo đơn mua hàng</Button>
                </Link>
              )}
            </Space>
          </div>
        )}
        columns={columns}
        dataSource={rows}
        loading={loading}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: (page, pageSize) => {
            setPagination((prev) => ({
              ...prev,
              current: page,
              pageSize,
            }))
          },
        }}
        cardClassName="!rounded-2xl shadow"
      />
    </MainLayout>
  )
}