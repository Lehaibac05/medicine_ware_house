import type { TableProps } from "antd"
import BaseTable from "../base/BaseTable"

type DataTableProps<T extends object> = TableProps<T>

export default function DataTable<T extends object>(props: DataTableProps<T>) {
  return (
    <BaseTable
      {...props}
      cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
    />
  )
}
