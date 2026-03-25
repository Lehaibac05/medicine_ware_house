import { Alert, Button, Space, Table, Tag, Typography, Upload, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import type { UploadFile } from "antd/es/upload/interface"
import { useMemo, useState } from "react"
import BaseModal from "../../../components/base/BaseModal"
import {
  commitImportUsers,
  previewImportUsers,
  type BulkUserImportResponse,
  type BulkUserImportRowResult,
} from "../../../services/users"

const { Dragger } = Upload
const { Text } = Typography

type UserImportModalProps = {
  open: boolean
  onCancel: () => void
  onImported: () => Promise<void> | void
}

type ImportRow = BulkUserImportRowResult & {
  key: string
}

const translateImportError = (error: string): string => {
  const normalized = error.trim()

  if (normalized === "Username is required") return "Thiếu tên đăng nhập"
  if (normalized === "Full name is required") return "Thiếu họ và tên"
  if (normalized === "Email is required") return "Thiếu email"
  if (normalized === "Email format is invalid") return "Email không đúng định dạng"
  if (normalized === "Status is required") return "Thiếu trạng thái"
  if (normalized === "Duplicate username in file") return "Trùng tên đăng nhập trong file"
  if (normalized === "Duplicate email in file") return "Trùng email trong file"
  if (normalized === "Username already exists in system") return "Tên đăng nhập đã tồn tại trong hệ thống"
  if (normalized === "Email already exists in system") return "Email đã tồn tại trong hệ thống"
  if (normalized === "Role is required") return "Thiếu vai trò"
  if (normalized === "Invalid role_id value") return "Giá trị role_id không hợp lệ"

  if (normalized.startsWith("Invalid status.")) {
    return "Trạng thái không hợp lệ. Chỉ chấp nhận ACTIVE, INACTIVE, LOCKED"
  }

  if (normalized.startsWith("Invalid role value.")) {
    return "Vai trò không hợp lệ. Chỉ chấp nhận: ADMIN, WAREHOUSE_MANAGER, WAREHOUSE_STAFF, ACCOUNTANT, REQUESTER"
  }

  if (normalized.startsWith("Mapped role not found in database for value:")) {
    const roleValue = normalized.split(":")[1]?.trim()
    return roleValue
      ? `Vai trò ${roleValue} chưa được cấu hình trong hệ thống`
      : "Vai trò chưa được cấu hình trong hệ thống"
  }

  if (normalized.startsWith("Role not found for role_id=")) {
    const roleId = normalized.split("=")[1]?.trim()
    return roleId
      ? `Không tìm thấy vai trò với role_id=${roleId}`
      : "Không tìm thấy vai trò theo role_id"
  }

  return normalized
}

const translateImportMessage = (message?: string): string => {
  if (!message) return ""

  const normalized = message.trim()
  if (normalized === "Preview completed") return "Đã preview file thành công"
  if (normalized === "Import completed successfully") return "Import người dùng thành công"
  if (normalized === "CSV file is empty") return "File CSV đang trống"
  if (normalized.startsWith("Import aborted:")) {
    return "Import bị dừng vì file còn lỗi. Vui lòng sửa lỗi và thử lại"
  }

  return normalized
}

function UserImportModal({ open, onCancel, onImported }: UserImportModalProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [previewLoading, setPreviewLoading] = useState(false)
  const [commitLoading, setCommitLoading] = useState(false)
  const [preview, setPreview] = useState<BulkUserImportResponse | null>(null)

  const selectedFile = useMemo(() => fileList[0]?.originFileObj ?? null, [fileList])

  const resetState = () => {
    setFileList([])
    setPreview(null)
    setPreviewLoading(false)
    setCommitLoading(false)
  }

  const handleCancel = () => {
    resetState()
    onCancel()
  }

  const handlePreview = async () => {
    if (!selectedFile) {
      messageApi.warning("Vui lòng chọn file CSV trước")
      return
    }

    try {
      setPreviewLoading(true)
      const result = await previewImportUsers(selectedFile)
      setPreview(result)
      messageApi.success("Preview hoàn tất")
    } catch {
      messageApi.error("Không thể preview file import")
    } finally {
      setPreviewLoading(false)
    }
  }

  const handleCommit = async () => {
    if (!selectedFile) {
      messageApi.warning("Vui lòng chọn file CSV trước")
      return
    }

    if (!preview) {
      messageApi.warning("Vui lòng preview trước khi commit")
      return
    }

    if (preview.invalidRows > 0) {
      messageApi.error("Không thể commit khi còn dòng lỗi")
      return
    }

    try {
      setCommitLoading(true)
      const result = await commitImportUsers(selectedFile)
      setPreview(result)
      messageApi.success(`Import thành công ${result.createdRows} người dùng`)
      await onImported()
    } catch {
      messageApi.error("Không thể commit import")
    } finally {
      setCommitLoading(false)
    }
  }

  const rows: ImportRow[] = (preview?.rows ?? []).map((row) => ({
    ...row,
    key: `${row.rowNumber}-${row.username}-${row.email}`,
  }))

  const columns: ColumnsType<ImportRow> = [
    {
      title: "Dòng",
      dataIndex: "rowNumber",
      key: "rowNumber",
      width: 70,
    },
    {
      title: "Username",
      dataIndex: "username",
      key: "username",
      width: 160,
    },
    {
      title: "Email",
      dataIndex: "email",
      key: "email",
      width: 220,
    },
    {
      title: "Vai trò",
      dataIndex: "roleInput",
      key: "roleInput",
      width: 150,
      render: (value?: string) => value || "-",
    },
    {
      title: "Trạng thái",
      dataIndex: "valid",
      key: "valid",
      width: 110,
      render: (value: boolean) =>
        value ? <Tag color="green">Hợp lệ</Tag> : <Tag color="red">Lỗi</Tag>,
    },
    {
      title: "Chi tiết lỗi",
      dataIndex: "errors",
      key: "errors",
      render: (value: string[]) =>
        value?.length
          ? value.map(translateImportError).join("; ")
          : <span className="text-slate-400">-</span>,
    },
  ]

  return (
    <>
      {contextHolder}
      <BaseModal
        open={open}
        title="Import người dùng hàng loạt"
        onCancel={handleCancel}
        width={980}
        footer={
          <Space>
            <Button onClick={handleCancel}>Đóng</Button>
            <Button onClick={handlePreview} loading={previewLoading}>
              Preview
            </Button>
            <Button
              type="primary"
              onClick={handleCommit}
              loading={commitLoading}
              disabled={!preview || preview.invalidRows > 0}
            >
              Commit import
            </Button>
          </Space>
        }
      >
        <div className="space-y-4">
          <Dragger
            accept=".csv"
            maxCount={1}
            multiple={false}
            beforeUpload={() => false}
            fileList={fileList}
            onChange={(info) => {
              setFileList(info.fileList.slice(-1))
              setPreview(null)
            }}
          >
            <p className="ant-upload-text">Chọn file CSV để import user</p>
            <p className="ant-upload-hint">
              Hệ thống sẽ preview và validate trước khi cho commit.
            </p>
          </Dragger>

          {preview ? (
            <Alert
              type={preview.invalidRows > 0 ? "warning" : "success"}
              showIcon
              message={translateImportMessage(preview.message)}
              description={
                <div>
                  <Text>Tổng dòng: {preview.totalRows}</Text>
                  <br />
                  <Text>Hợp lệ: {preview.validRows}</Text>
                  <br />
                  <Text>Lỗi: {preview.invalidRows}</Text>
                  <br />
                  <Text>Đã tạo: {preview.createdRows}</Text>
                </div>
              }
            />
          ) : null}

          <Table<ImportRow>
            size="small"
            columns={columns}
            dataSource={rows}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 900 }}
          />
        </div>
      </BaseModal>
    </>
  )
}

export default UserImportModal
