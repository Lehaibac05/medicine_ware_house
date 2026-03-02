import { Card, Col, Row, Statistic, Typography, Spin } from 'antd'
import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { getAlertStats, type AlertStats } from '../../../services/alerts'

const { Text } = Typography

type StatItem = {
  label: string
  value: number
  note?: ReactNode
}

function AlertsStatsGrid() {
  const [stats, setStats] = useState<StatItem[]>([
    { label: 'Low stock alerts', value: 0, note: 'Below safety threshold' },
    { label: 'Expiring soon', value: 0, note: 'Next 30 days' },
    { label: 'System warnings', value: 0, note: 'Integrations & jobs' },
    { label: 'Expired notifications', value: 0, note: 'Needs action' },
  ])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadStats()
  }, [])

  const loadStats = async () => {
    try {
      setLoading(true)
      const data: AlertStats = await getAlertStats()
      
      setStats([
        { label: 'Low stock alerts', value: data.lowStockCount, note: 'Below safety threshold' },
        { label: 'Expiring soon', value: data.expiringSoonCount, note: 'Next 30 days' },
        { label: 'System warnings', value: data.systemWarningsCount, note: 'Integrations & jobs' },
        { label: 'Expired notifications', value: data.expiredCount, note: 'Needs action' },
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

export default AlertsStatsGrid

