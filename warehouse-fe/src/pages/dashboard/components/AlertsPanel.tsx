import { Badge, Card, List, Tag, Typography } from 'antd'
import type { BadgeProps } from 'antd'

const { Text, Title } = Typography

const alerts = [
  {
    title: 'Lô BXC-102 gần hết hạn',
    desc: 'Còn 18 ngày, ưu tiên xuất kho tuyến A.',
    level: 'warning' as BadgeProps['status'],
  },
  {
    title: 'Tủ lạnh kho 2 lệch nhiệt',
    desc: 'Nhiệt độ 8.2°C trong 12 phút.',
    level: 'processing' as BadgeProps['status'],
  },
  {
    title: 'Thiếu hàng nhóm kháng sinh',
    desc: '3 mặt hàng dưới ngưỡng an toàn.',
    level: 'error' as BadgeProps['status'],
  },
  {
    title: 'Đơn cấp cứu cần duyệt',
    desc: '2 đơn hàng từ khoa ICU.',
    level: 'default' as BadgeProps['status'],
  },
]

function AlertsPanel() {
  return (
    <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            System Alerts
          </Text>

          <Title level={4} className="!m-0">
            Cảnh báo hệ thống
          </Title>
        </div>

        <Tag color="volcano">4 mới</Tag>
      </div>

      <List
        dataSource={alerts}
        renderItem={(item) => (
          <List.Item className="!border-0 !p-0 !pb-3 last:!pb-0">
            <div className="w-full rounded-xl border border-slate-200 p-3">
              <div className="flex items-center gap-2">
                <Badge status={item.level} />
                <Text strong>{item.title}</Text>
              </div>

              <Text className="mt-1.5 block text-slate-500">
                {item.desc}
              </Text>
            </div>
          </List.Item>
        )}
      />
    </Card>
  )
}

export default AlertsPanel
