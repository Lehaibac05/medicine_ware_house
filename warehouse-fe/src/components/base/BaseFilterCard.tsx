import { Card } from 'antd'
import type { ReactNode } from 'react'

type BaseFilterCardProps = {
  children: ReactNode
  actions?: ReactNode
  className?: string
}

function BaseFilterCard({
  children,
  actions,
  className,
}: BaseFilterCardProps) {
  return (
    <Card
      className={
        className ||
        '!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]'
      }
    >
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-[1fr_1fr_1fr_1fr_auto] items-end">
        {children}
        {actions}
      </div>
    </Card>
  )
}

export default BaseFilterCard
