import { Button, Space, Tag, Typography, message, Modal } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState, useMemo } from 'react'
import BaseTable from '../../../components/base/BaseTable'
import { getOrders, updateOrderStatus, type Order } from '../../../services/orders'
import type { OrderFilters } from '../OrdersPage'
import dayjs from 'dayjs'

const { Text } = Typography

export type OrderRow = {
  key: string
  orderId: number
  orderDate: string
  userName: string
  status: string
  itemCount: number
  totalAmount: number
  items: string
}

const statusTag = (status: string) => {
  const upper = status.toUpperCase()
  if (upper === 'COMPLETED') return <Tag color="green">Completed</Tag>
  if (upper === 'PENDING') return <Tag color="gold">Pending</Tag>
  if (upper === 'PROCESSING') return <Tag color="blue">Processing</Tag>
  if (upper === 'CANCELLED') return <Tag color="red">Cancelled</Tag>
  return <Tag color="default">{status}</Tag>
}

type OrdersTableProps = {
  filters: OrderFilters
}

function OrdersTable({ filters }: OrdersTableProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const [orders, setOrders] = useState<OrderRow[]>([])
  const [loading, setLoading] = useState(false)
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  useEffect(() => {
    loadOrders()
  }, [])

  const loadOrders = async () => {
    try {
      setLoading(true)
      const data = await getOrders()
      
      const mapped: OrderRow[] = data.map((order: Order) => {
        const itemsSummary = order.items
          .map(item => `${item.medicineName || 'Medicine'} (${item.quantity})`)
          .join(', ')
        
        return {
          key: order.orderId.toString(),
          orderId: order.orderId,
          orderDate: new Date(order.orderDate).toLocaleDateString('en-GB'),
          userName: order.userName || 'Unknown User',
          status: order.status,
          itemCount: order.items.length,
          totalAmount: order.totalAmount,
          items: itemsSummary || '—',
        }
      })
      
      setOrders(mapped)
    } catch (error) {
      messageApi.error('Failed to load orders')
      console.error('Load orders error:', error)
    } finally {
      setLoading(false)
    }
  }
  const filteredAndSortedOrders = useMemo(() => {
    let result = [...orders]
    
    // Filter by status
    if (filters.status !== 'all') {
      result = result.filter(order => order.status.toUpperCase() === filters.status)
    }
    
    // Filter by date range
    if (filters.dateRange && filters.dateRange[0] && filters.dateRange[1]) {
      const startDate = filters.dateRange[0].startOf('day')
      const endDate = filters.dateRange[1].endOf('day')
      
      result = result.filter(order => {
        const orderDate = dayjs(order.orderDate, 'DD/MM/YYYY')
        return orderDate.isAfter(startDate) && orderDate.isBefore(endDate)
      })
    }
    
    // Filter by search text
    if (filters.searchText) {
      const searchLower = filters.searchText.toLowerCase()
      result = result.filter(order => 
        order.orderId.toString().includes(searchLower) ||
        order.userName.toLowerCase().includes(searchLower) ||
        order.items.toLowerCase().includes(searchLower)
      )
    }
    
    // Sort
    if (filters.sortBy === 'date-desc') {
      result.sort((a, b) => {
        const dateA = dayjs(a.orderDate, 'DD/MM/YYYY')
        const dateB = dayjs(b.orderDate, 'DD/MM/YYYY')
        return dateB.diff(dateA)
      })
    } else if (filters.sortBy === 'date-asc') {
      result.sort((a, b) => {
        const dateA = dayjs(a.orderDate, 'DD/MM/YYYY')
        const dateB = dayjs(b.orderDate, 'DD/MM/YYYY')
        return dateA.diff(dateB)
      })
    } else if (filters.sortBy === 'amount-desc') {
      result.sort((a, b) => b.totalAmount - a.totalAmount)
    } else if (filters.sortBy === 'amount-asc') {
      result.sort((a, b) => a.totalAmount - b.totalAmount)
    }
    
    return result
  }, [orders, filters])
  const handleUpdateStatus = async (orderId: number, newStatus: string) => {
    try {
      setUpdatingId(orderId)
      await updateOrderStatus(orderId, newStatus)
      messageApi.success(`Order status updated to ${newStatus}`)
      await loadOrders()
    } catch (error) {
      messageApi.error('Failed to update order status')
      console.error('Update status error:', error)
    } finally {
      setUpdatingId(null)
    }
  }

  const showOrderDetails = (record: OrderRow) => {
    Modal.info({
      title: `Order #${record.orderId} Details`,
      width: 600,
      content: (
        <div className="space-y-3 mt-4">
          <div>
            <Text strong>Order Date:</Text> {record.orderDate}
          </div>
          <div>
            <Text strong>Customer:</Text> {record.userName}
          </div>
          <div>
            <Text strong>Status:</Text> {statusTag(record.status)}
          </div>
          <div>
            <Text strong>Items ({record.itemCount}):</Text>
            <div className="mt-1 text-sm text-slate-600">{record.items}</div>
          </div>
          <div>
            <Text strong>Total Amount:</Text> ${record.totalAmount.toFixed(2)}
          </div>
        </div>
      ),
    })
  }

  const columns: ColumnsType<OrderRow> = [
    {
      title: 'Order ID',
      dataIndex: 'orderId',
      key: 'orderId',
      render: (value: number) => <Text strong>ORD-{value.toString().padStart(4, '0')}</Text>,
      width: 120,
    },
    {
      title: 'Order Date',
      dataIndex: 'orderDate',
      key: 'orderDate',
      width: 120,
    },
    {
      title: 'Customer',
      dataIndex: 'userName',
      key: 'userName',
      ellipsis: true,
    },
    {
      title: 'Items',
      dataIndex: 'itemCount',
      key: 'itemCount',
      width: 80,
      render: (value: number) => `${value} item${value > 1 ? 's' : ''}`,
    },
    {
      title: 'Total Amount',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 140,
      render: (value: number) => <Text strong>${value.toFixed(2)}</Text>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) => statusTag(value),
      width: 130,
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 240,
      render: (_: unknown, record: OrderRow) => (
        <Space>
          <Button 
            size="small"
            onClick={() => showOrderDetails(record)}
          >
            View
          </Button>
          {record.status === 'PENDING' && (
            <Button 
              size="small" 
              type="primary"
              loading={updatingId === record.orderId}
              onClick={() => handleUpdateStatus(record.orderId, 'PROCESSING')}
            >
              Process
            </Button>
          )}
          {record.status === 'PROCESSING' && (
            <Button 
              size="small" 
              type="primary"
              loading={updatingId === record.orderId}
              onClick={() => handleUpdateStatus(record.orderId, 'COMPLETED')}
            >
              Complete
            </Button>
          )}
          {(record.status === 'PENDING' || record.status === 'PROCESSING') && (
            <Button 
              size="small" 
              danger
              loading={updatingId === record.orderId}
              onClick={() => handleUpdateStatus(record.orderId, 'CANCELLED')}
            >
              Cancel
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <>
      {contextHolder}
      <BaseTable
        columns={columns}
        dataSource={filteredAndSortedOrders}
        loading={loading}
        scroll={{ x: 1100 }}
        cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
      />
    </>
  )
}

export default OrdersTable
