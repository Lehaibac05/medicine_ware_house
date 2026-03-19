import { Button, Card, Space } from "antd"
import type { ReactNode } from "react"

type FilterPanelProps = {
  children: ReactNode
  onApply?: () => void
  onReset?: () => void
  applyText?: string
  extraActions?: ReactNode
}

export default function FilterPanel({
  children,
  onApply,
  onReset,
  applyText = "Apply",
  extraActions,
}: FilterPanelProps) {
  return (
    <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
      <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{children}</div>
        <Space className="justify-end">
          {extraActions}
          {onReset && <Button onClick={onReset}>Reset</Button>}
          {onApply && (
            <Button type="primary" onClick={onApply}>
              {applyText}
            </Button>
          )}
        </Space>
      </div>
    </Card>
  )
}
