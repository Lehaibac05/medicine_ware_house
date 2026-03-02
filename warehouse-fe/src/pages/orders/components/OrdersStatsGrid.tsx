import { Card, Col, Row, Statistic, Typography, Spin } from 'antd'
import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { getOrders } from '../../../services/orders'

const { Text } = Typography

type StatItem = {
  label: string
  value: number | string
  note?: ReactNode
  prefix?: string
}

function OrdersStatsGrid() {
  const [stats, setStats] = useState<StatItem[]>([
    { label: 'Pending orders', value: 0, note: 'Awaiting processing' },
    { label: 'Completed orders', value: 0, note: 'Successfully delivered' },
    { label: 'Cancelled orders', value: 0, note: 'Cancelled or rejected' },
    { label: 'Total revenue', value: '$0', note: 'From completed orders' },
  ])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadStats()
  }, [])

  const loadStats = async () => {
    try {
      setLoading(true)
      const orders = await getOrders()
      
      const pending = orders.filter(o => o.status === 'PENDING').length
      const completed = orders.filter(o => o.status === 'COMPLETED').length
      const cancelled = orders.filter(o => o.status === 'CANCELLED').length
      
      const totalRevenue = orders
        .filter(o => o.status === 'COMPLETED')
        .reduce((sum, order) => sum + order.totalAmount, 0)
      
      setStats([
        { label: 'Pending orders', value: pending, note: 'Awaiting processing' },
        { label: 'Completed orders', value: completed, note: 'Successfully delivered' },
        { label: 'Cancelled orders', value: cancelled,  note: 'Cancelled or rejected' },
        { 
          label: 'Total revenue', 
          value: `$${totalRevenue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`, 
          note: 'From completed orders' 
        },
      ])
    } catch (error) {
      console.error('Load stats error:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-8">
        <Spin size="large" />
      </div>
    )
  }

  return (
    <Row gutter={[16, 16]}>
      {stats.map((stat) => (
        <Col key={stat.label} xs={24} sm={12} xl={6}>
          <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              {stat.label}
            </Text>
            <Statistic
              value={stat.value}
              valueStyle={{ fontSize: 28, fontWeight: 700, color: '#0f172a' }}
            />
            {stat.note ? (
              <Text className="text-[13px] text-slate-500">{stat.note}</Text>
            ) : null}
          </Card>
        </Col>
      ))}
    </Row>
  )
}

export default OrdersStatsGrid
