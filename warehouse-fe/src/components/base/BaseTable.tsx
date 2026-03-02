import { Card, Table } from "antd";
import type { TableProps, TablePaginationConfig } from "antd";

type BaseTableProps<T extends object> = TableProps<T> & {
  cardClassName?: string;
  pageSize?: number;
};

function BaseTable<T extends object>({
  columns,
  dataSource,
  pagination,
  pageSize = 10,
  cardClassName,
  ...rest
}: BaseTableProps<T>) {
  const finalPagination =
    pagination === false
      ? false
      : {
          pageSize,
          showSizeChanger: true,
          pageSizeOptions: ["5", "10", "20", "50"],
          showTotal: (total: number) => `Total ${total} items`,
          position: ["bottomCenter"] as TablePaginationConfig["position"],
          ...(typeof pagination === "object" ? pagination : {}),
        };

  return (
    <Card className={cardClassName || "!rounded-2xl shadow"}>
      <Table<T>
        columns={columns}
        dataSource={dataSource}
        pagination={finalPagination}
        className="mt-1"
        {...rest}
      />
    </Card>
  );
}

export default BaseTable;
