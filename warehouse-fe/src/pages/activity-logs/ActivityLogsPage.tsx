import { Button, Flex, Input, Tag, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useCallback, useEffect, useMemo, useState } from "react"
import MainLayout from "../../layouts/MainLayout"
import BaseTable from "../../components/base/BaseTable"
import {
  getActivityLogs,
  type ActivityLogItem,
} from "../../services/activityLogs"

const { Text, Title } = Typography

type ActivityLogRow = {
  key: string
  logId: number
  action: string
  targetObject: string
  timestamp: string
  ipAddress: string
  username: string
  fullName: string
}

const actionTagColor = (action: string) => {
  if (action.includes("CREATE")) return "green"
  if (action.includes("UPDATE") || action.includes("APPROVE")) return "blue"
  if (action.includes("REJECT") || action.includes("DELETE")) return "red"
  if (action.includes("SCAN") || action.includes("IMPORT")) return "purple"
  return "default"
}

const ACTION_LABELS: Record<string, string> = {
  CREATE_USER: "Tạo người dùng",
  UPDATE_USER: "Cập nhật người dùng",
  IMPORT_USERS_COMMIT: "Import người dùng",
  SCAN_ALERTS: "Quét cảnh báo",
  RESOLVE_ALERT: "Xử lý cảnh báo",
  UPDATE_ALERT_STATUS: "Cập nhật trạng thái cảnh báo",
  CREATE_MEDICINE_REQUEST: "Tạo yêu cầu nhập thuốc",
  APPROVE_MEDICINE_REQUEST: "Phê duyệt yêu cầu nhập thuốc",
  REJECT_MEDICINE_REQUEST: "Từ chối yêu cầu nhập thuốc",
}

const toActionLabel = (action: string) => ACTION_LABELS[action] ?? action

function ActivityLogsPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<ActivityLogItem[]>([])
  const [searchAction, setSearchAction] = useState("")
  const [appliedAction, setAppliedAction] = useState("")
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [totalItems, setTotalItems] = useState(0)

  const loadLogs = useCallback(async () => {
    setLoading(true)
    try {
      const pageResponse = await getActivityLogs({
        page: currentPage - 1,
        size: pageSize,
        action: appliedAction || undefined,
      })
      setData(pageResponse.content)
      setTotalItems(pageResponse.totalElements)
    } catch {
      messageApi.error("Không tải được activity log")
    } finally {
      setLoading(false)
    }
  }, [currentPage, pageSize, appliedAction, messageApi])

  useEffect(() => {
    void loadLogs()
  }, [loadLogs])

  const rows = useMemo<ActivityLogRow[]>(
    () =>
      data.map((item) => ({
        key: String(item.logId),
        logId: item.logId,
        action: item.action,
        targetObject: item.targetObject,
        timestamp: item.timestamp
          ? new Intl.DateTimeFormat("vi-VN", {
              dateStyle: "short",
              timeStyle: "medium",
            }).format(new Date(item.timestamp))
          : "-",
        ipAddress: item.ipAddress || "-",
        username: item.username || "system",
        fullName: item.fullName || "-",
      })),
    [data],
  )

  const columns: ColumnsType<ActivityLogRow> = [
    {
      title: "Mã log",
      dataIndex: "logId",
      key: "logId",
      width: 90,
      render: (value: number) => <Text strong>{value}</Text>,
    },
    {
      title: "Hành động",
      dataIndex: "action",
      key: "action",
      width: 200,
      render: (value: string) => (
        <Tag color={actionTagColor(value)}>
          {toActionLabel(value)}
        </Tag>
      ),
    },
    {
      title: "Đối tượng",
      dataIndex: "targetObject",
      key: "targetObject",
      width: 220,
    },
    {
      title: "Người thực hiện",
      key: "user",
      width: 220,
      render: (_, row) => (
        <div className="flex flex-col">
          <Text>{row.fullName}</Text>
          <Text className="text-xs text-slate-500">{row.username}</Text>
        </div>
      ),
    },
    {
      title: "IP",
      dataIndex: "ipAddress",
      key: "ipAddress",
      width: 150,
    },
    {
      title: "Thời gian",
      dataIndex: "timestamp",
      key: "timestamp",
      width: 170,
    },
  ]

  const tableHeader = (
    <Flex justify="space-between" align="start" gap={16} wrap>
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Nhật ký hoạt động
        </Text>
        <Title level={5} className="!m-0 !mt-1">
          Nhật ký hoạt động hệ thống
        </Title>
      </div>

      <div className="w-full lg:w-auto lg:min-w-[560px]">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
          <Input.Search
            placeholder="Lọc theo hành động, ví dụ: SCAN_ALERTS"
            className="w-full sm:min-w-[360px]"
            size="large"
            value={searchAction}
            onChange={(event) => setSearchAction(event.target.value)}
            onSearch={(value) => {
              setAppliedAction(value.trim())
              setCurrentPage(1)
            }}
            allowClear
          />
          <Button
            onClick={() => {
              setSearchAction("")
              setAppliedAction("")
              setCurrentPage(1)
            }}
          >
            Xóa lọc
          </Button>
        </div>
      </div>
    </Flex>
  )

  return (
    <MainLayout>
      {contextHolder}
      <BaseTable
        title={() => tableHeader}
        columns={columns}
        dataSource={rows}
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize,
          total: totalItems,
          onChange: (page, nextPageSize) => {
            setCurrentPage(page)
            if (nextPageSize && nextPageSize !== pageSize) {
              setPageSize(nextPageSize)
              setCurrentPage(1)
            }
          },
        }}
      />
    </MainLayout>
  )
}

export default ActivityLogsPage
