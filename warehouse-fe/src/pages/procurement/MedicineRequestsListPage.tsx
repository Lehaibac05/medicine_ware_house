import { Button, DatePicker, Input, Select, Space, Tag, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useCallback, useEffect, useMemo, useState } from "react"
import dayjs from "dayjs"
import BaseFilterCard from "../../components/base/BaseFilterCard"
import { Link } from "react-router-dom"
import BaseTable from "../../components/base/BaseTable"
import MainLayout from "../../layouts/MainLayout"
import {
  approveMedicineRequest,
  getMedicineRequestsPage,
  getMyMedicineRequestsPage,
  rejectMedicineRequest,
  type GetMedicineRequestsParams,
  type MedicineRequest,
  type MedicineRequestStatus,
} from "../../services/medicineRequests"
import { hasAnyRole } from "../../utils/auth"

const { Text } = Typography
const { RangePicker } = DatePicker

type RequestRow = {
  key: string
  requestId: number
  medicine: string
  quantity: number
  requestedBy: string
  createdDate: string
  status: "PENDING" | "APPROVED" | "REJECTED"
}

type RequestFilters = {
  medicineName: string
  status?: MedicineRequestStatus
  dateRange: [string | null, string | null]
}

const statusTag = (status: RequestRow["status"]) => {
  if (status === "APPROVED") return <Tag color="green">APPROVED</Tag>
  if (status === "REJECTED") return <Tag color="red">REJECTED</Tag>
  return <Tag color="gold">PENDING</Tag>
}

const normalizeStatus = (status?: string): RequestRow["status"] => {
  const normalized = status?.trim().toUpperCase()
  if (normalized === "APPROVED") return "APPROVED"
  if (normalized === "REJECTED") return "REJECTED"
  return "PENDING"
}

export default function MedicineRequestsListPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [requests, setRequests] = useState<MedicineRequest[]>([])
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [totalItems, setTotalItems] = useState(0)
  const [actingRequestId, setActingRequestId] = useState<number | null>(null)
  const [actingType, setActingType] = useState<"approve" | "reject" | null>(null)
  const [draftFilters, setDraftFilters] = useState<RequestFilters>({
    medicineName: "",
    status: undefined,
    dateRange: [null, null],
  })
  const [appliedFilters, setAppliedFilters] = useState<RequestFilters>({
    medicineName: "",
    status: undefined,
    dateRange: [null, null],
  })
  const isManagerView = hasAnyRole(["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"])

  const loadRequests = useCallback(async () => {
    try {
      setLoading(true)
      const params: GetMedicineRequestsParams = {
        page: currentPage - 1,
        size: pageSize,
        medicineName: appliedFilters.medicineName || undefined,
        status: appliedFilters.status,
        startDate: appliedFilters.dateRange[0]
          ? `${appliedFilters.dateRange[0]}T00:00:00`
          : undefined,
        endDate: appliedFilters.dateRange[1]
          ? `${appliedFilters.dateRange[1]}T23:59:59`
          : undefined,
      }
      const page = isManagerView
        ? await getMedicineRequestsPage(params)
        : await getMyMedicineRequestsPage(params)
      setRequests(page.content)
      setTotalItems(page.totalElements)

      const maxPage = Math.max(page.totalPages, 1)
      if (currentPage > maxPage) {
        setCurrentPage(maxPage)
      }
    } catch {
      messageApi.error("Failed to load medicine requests")
    } finally {
      setLoading(false)
    }
  }, [appliedFilters, currentPage, isManagerView, messageApi, pageSize])

  useEffect(() => {
    void loadRequests()
  }, [loadRequests])

  const applyFilters = () => {
    setCurrentPage(1)
    setAppliedFilters({
      medicineName: draftFilters.medicineName.trim(),
      status: draftFilters.status,
      dateRange: draftFilters.dateRange,
    })
  }

  const resetFilters = () => {
    const emptyFilters = {
      medicineName: "",
      status: undefined,
      dateRange: [null, null] as [null, null],
    }
    setDraftFilters(emptyFilters)
    setAppliedFilters(emptyFilters)
    setCurrentPage(1)
  }

  const onApprove = async (requestId: number) => {
    try {
      setActingRequestId(requestId)
      setActingType("approve")
      await approveMedicineRequest(requestId)
      messageApi.success("Request approved")
      await loadRequests()
    } catch {
      messageApi.error("Failed to approve request")
    } finally {
      setActingRequestId(null)
      setActingType(null)
    }
  }

  const onReject = async (requestId: number) => {
    try {
      setActingRequestId(requestId)
      setActingType("reject")
      await rejectMedicineRequest(requestId)
      messageApi.success("Request rejected")
      await loadRequests()
    } catch {
      messageApi.error("Failed to reject request")
    } finally {
      setActingRequestId(null)
      setActingType(null)
    }
  }

  const rows = useMemo<RequestRow[]>(
    () =>
      requests.map((request) => {
        const first = request.items[0]
        const medLabel = first?.medicineName
          ? request.items.length > 1
            ? `${first.medicineName} +${request.items.length - 1}`
            : first.medicineName
          : `Medicine #${first?.medicineId ?? "-"}`

        const quantity = request.items.reduce((acc, item) => acc + item.quantity, 0)

        return {
          key: String(request.requestId),
          requestId: request.requestId,
          medicine: medLabel,
          quantity,
          requestedBy: request.requestedBy,
          createdDate: new Date(request.createdDate).toLocaleDateString("en-GB"),
          status: normalizeStatus(request.status),
        }
      }),
    [requests],
  )

  const columns: ColumnsType<RequestRow> = [
    {
      title: "Request ID",
      dataIndex: "requestId",
      render: (value: number) => <Text strong>MR-{String(value).padStart(4, "0")}</Text>,
      width: 130,
    },
    { title: "Medicine", dataIndex: "medicine", key: "medicine" },
    {
      title: "Quantity",
      dataIndex: "quantity",
      key: "quantity",
      render: (value: number) => value.toLocaleString(),
      width: 120,
    },
    { title: "Requested By", dataIndex: "requestedBy", key: "requestedBy", width: 180 },
    { title: "Created Date", dataIndex: "createdDate", key: "createdDate", width: 140 },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (value: RequestRow["status"]) => statusTag(value),
      width: 120,
    },
    {
      title: "Action",
      key: "action",
      width: 260,
      render: (_, record) => (
        <Space>
          {isManagerView ? (
            <>
              <Button
                size="small"
                onClick={() => void onApprove(record.requestId)}
                loading={actingRequestId === record.requestId && actingType === "approve"}
              >
                Approve
              </Button>
              <Button
                size="small"
                danger
                onClick={() => void onReject(record.requestId)}
                loading={actingRequestId === record.requestId && actingType === "reject"}
              >
                Reject
              </Button>
            </>
          ) : null}
          {record.status === "APPROVED" ? (
            <Link to={`/purchase-orders/create?requestId=${record.requestId}`}>
              <Button size="small" type="primary">
                Create PO
              </Button>
            </Link>
          ) : null}
        </Space>
      ),
    },
  ]

  return (
    <MainLayout>
      {contextHolder}
        <>
          <BaseFilterCard
            actions={
              <div className="flex gap-2">
                <Button className="h-[40px] flex-1" onClick={resetFilters}>
                  Reset
                </Button>
                <Button type="primary" className="h-[40px] flex-1" onClick={applyFilters}>
                  Apply
                </Button>
              </div>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Medicine</Text>
              <Input
                placeholder="Enter medicine name"
                value={draftFilters.medicineName}
                onChange={(event) =>
                  setDraftFilters((prev) => ({
                    ...prev,
                    medicineName: event.target.value,
                  }))
                }
                onPressEnter={applyFilters}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Status</Text>
              <Select
                allowClear
                placeholder="Select status"
                value={draftFilters.status}
                onChange={(value) =>
                  setDraftFilters((prev) => ({
                    ...prev,
                    status: value,
                  }))
                }
                options={[
                  { label: "Pending", value: "PENDING" },
                  { label: "Approved", value: "APPROVED" },
                  { label: "Rejected", value: "REJECTED" },
                ]}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Created Date</Text>
              <RangePicker
                className="w-full"
                format="DD/MM/YYYY"
                value={[
                  draftFilters.dateRange[0] ? dayjs(draftFilters.dateRange[0]) : null,
                  draftFilters.dateRange[1] ? dayjs(draftFilters.dateRange[1]) : null,
                ]}
                onChange={(_, dateStrings) =>
                  setDraftFilters((prev) => ({
                    ...prev,
                    dateRange: [
                      dateStrings[0] ? dayjs(dateStrings[0], "DD/MM/YYYY").format("YYYY-MM-DD") : null,
                      dateStrings[1] ? dayjs(dateStrings[1], "DD/MM/YYYY").format("YYYY-MM-DD") : null,
                    ],
                  }))
                }
              />
            </div>
          </BaseFilterCard>

          <BaseTable
            title={() => (
              <div className="flex items-center justify-between">
                <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                  Request list
                </Text>
                <Space>
                  {isManagerView ? (
                    <Link to="/medicine-requests/approval">
                      <Button>Approval Queue</Button>
                    </Link>
                  ) : null}
                  <Link to="/medicine-requests/create">
                    <Button type="primary">Create Request</Button>
                  </Link>
                </Space>
              </div>
            )}
            columns={columns}
            dataSource={rows}
            loading={loading}
            pagination={{
              current: currentPage,
              pageSize,
              total: totalItems,
              onChange: (page, nextPageSize) => {
                if (nextPageSize && nextPageSize !== pageSize) {
                  setPageSize(nextPageSize)
                  setCurrentPage(1)
                  return
                }
                setCurrentPage(page)
              },
            }}
            cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
          />
        </>
    </MainLayout>
  )
}
