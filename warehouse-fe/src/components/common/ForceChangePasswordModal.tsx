import { Button, Form, Input, Modal, Typography } from "antd"
import { useEffect, useState } from "react"
import { useToast } from "../../hooks/useToast"
import { forceChangePassword } from "../../services/users"
import {
  AUTH_STATE_CHANGE_EVENT,
  clearAuthToken,
  getAuthToken,
  isForceChangePasswordRequired,
  setForceChangePasswordRequired,
} from "../../utils/auth"

const { Text } = Typography

type FormValues = {
  newPassword: string
  confirmPassword: string
}

const passwordRules = [
  { required: true, message: "Vui lòng nhập mật khẩu mới" },
  { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự" },
]

function ForceChangePasswordModal() {
  const [form] = Form.useForm<FormValues>()
  const { toast, contextHolder } = useToast()
  const [open, setOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    const syncOpenState = () => {
      const token = getAuthToken()
      setOpen(Boolean(token) && isForceChangePasswordRequired())
    }

    syncOpenState()

    window.addEventListener(AUTH_STATE_CHANGE_EVENT, syncOpenState)
    window.addEventListener("storage", syncOpenState)

    return () => {
      window.removeEventListener(AUTH_STATE_CHANGE_EVENT, syncOpenState)
      window.removeEventListener("storage", syncOpenState)
    }
  }, [])

  const handleSubmit = async (values: FormValues) => {
    try {
      setSubmitting(true)
      await forceChangePassword({
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      })

      setForceChangePasswordRequired(false)
      setOpen(false)
      form.resetFields()
      toast.success("Đổi mật khẩu thành công. Bạn có thể tiếp tục sử dụng hệ thống.")
    } catch (error) {
      toast.error(error, "Không thể đổi mật khẩu bắt buộc")
    } finally {
      setSubmitting(false)
    }
  }

  const handleLogout = () => {
    clearAuthToken()
    setOpen(false)
    window.location.replace("/login")
  }

  if (!open) {
    return <>{contextHolder}</>
  }

  return (
    <>
      {contextHolder}
      <Modal
        title="Bắt buộc đổi mật khẩu"
        open={open}
        footer={null}
        closable={false}
        maskClosable={false}
        keyboard={false}
        width={520}
        centered
        destroyOnClose
      >
        <Text className="mb-4 block text-slate-600">
          Tài khoản của bạn được cấp mới hoặc đã được reset. Bạn phải đổi mật khẩu trước khi sử dụng hệ thống.
        </Text>

        <Form<FormValues> form={form} layout="vertical" onFinish={handleSubmit} autoComplete="off">
          <Form.Item label="Mật khẩu mới" name="newPassword" rules={passwordRules}>
            <Input.Password placeholder="Nhập mật khẩu mới" />
          </Form.Item>

          <Form.Item
            label="Xác nhận mật khẩu mới"
            name="confirmPassword"
            dependencies={["newPassword"]}
            rules={[
              { required: true, message: "Vui lòng xác nhận mật khẩu" },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue("newPassword") === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error("Mật khẩu xác nhận không khớp"))
                },
              }),
            ]}
          >
            <Input.Password placeholder="Nhập lại mật khẩu mới" />
          </Form.Item>

          <div className="mt-4 flex justify-end gap-2">
            <Button onClick={handleLogout} disabled={submitting}>
              Đăng xuất
            </Button>
            <Button type="primary" htmlType="submit" loading={submitting}>
              Cập nhật mật khẩu
            </Button>
          </div>
        </Form>
      </Modal>
    </>
  )
}

export default ForceChangePasswordModal
