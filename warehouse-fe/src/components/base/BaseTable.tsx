import { Card, Table } from 'antd'
import type { TableProps } from 'antd'

type BaseTableProps<T extends object> = TableProps<T> & {
  cardClassName?: string
}

function BaseTable<T extends object>({
  columns,
  dataSource,
  pagination = false,
  cardClassName,
  ...rest
}: BaseTableProps<T>) {
  return (
    <Card className={cardClassName || '!rounded-2xl shadow'}>
      <Table<T>
        columns={columns}
        dataSource={dataSource}
        pagination={pagination}
        className="mt-1"
        {...rest}
      />
    </Card>
  )
}

export default BaseTable
