import { Button, Card, Form, Input, Typography } from "antd"
import { useMemo, useState } from "react"
import MainLayout from "../../layouts/MainLayout"
import { useToast } from "../../hooks/useToast"
import { changePassword, updateUser } from "../../services/users"
import {
  getCurrentUserProfile,
  getPrimaryRole,
  getRoleLabel,
  setCurrentUserProfile,
} from "../../utils/auth"

const { Title, Text } = Typography

type ProfileFormValues = {
  fullName: string
  email: string
}

type PasswordFormValues = {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

function ProfilePage() {
  const { toast, contextHolder } = useToast()
  const [profileForm] = Form.useForm<ProfileFormValues>()
  const [passwordForm] = Form.useForm<PasswordFormValues>()
  const currentUser = useMemo(() => getCurrentUserProfile(), [])

  const [updatingProfile, setUpdatingProfile] = useState(false)
  const [updatingPassword, setUpdatingPassword] = useState(false)

  const onUpdateProfile = async (values: ProfileFormValues) => {
    if (!currentUser?.userId) {
      toast.warning("Không tìm thấy thông tin user để cập nhật")
      return
    }

    try {
      setUpdatingProfile(true)
      const updated = await updateUser(currentUser.userId, {
        fullName: values.fullName,
        email: values.email,
      })

      setCurrentUserProfile(updated)
      toast.success("Cập nhật thông tin cá nhân thành công")
    } catch (error) {
      toast.error(error, "Không thể cập nhật thông tin cá nhân")
    } finally {
      setUpdatingProfile(false)
    }
  }

  const onChangePassword = async (values: PasswordFormValues) => {
    try {
      setUpdatingPassword(true)
      await changePassword(values)
      passwordForm.resetFields()
      toast.success("Đổi mật khẩu thành công")
    } catch (error) {
      toast.error(error, "Không thể đổi mật khẩu")
    } finally {
      setUpdatingPassword(false)
    }
  }

  return (
    <MainLayout>
      {contextHolder}

      <div>
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Account
        </Text>
        <Title level={4} className="!mt-1 !mb-0 font-semibold">
          Thông tin cá nhân
        </Title>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="!rounded-2xl !border-0 shadow-[0_10px_24px_rgba(15,23,42,0.06)]">
          <Title level={5} className="!mt-0">
            Hồ sơ người dùng
          </Title>

          <Form<ProfileFormValues>
            form={profileForm}
            layout="vertical"
            initialValues={{
              fullName: currentUser?.fullName || "",
              email: currentUser?.email || "",
            }}
            onFinish={onUpdateProfile}
          >
            <Form.Item label="Tên đăng nhập">
              <Input value={currentUser?.username || "-"} disabled />
            </Form.Item>

            <Form.Item label="Vai trò">
              <Input value={getRoleLabel(getPrimaryRole())} disabled />
            </Form.Item>

            <Form.Item
              label="Họ và tên"
              name="fullName"
              rules={[{ required: true, message: "Vui lòng nhập họ và tên" }]}
            >
              <Input placeholder="Nhập họ và tên" />
            </Form.Item>

            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: "Vui lòng nhập email" },
                { type: "email", message: "Email không hợp lệ" },
              ]}
            >
              <Input placeholder="Nhập email" />
            </Form.Item>

            <div className="flex justify-end">
              <Button type="primary" htmlType="submit" loading={updatingProfile}>
                Lưu thay đổi
              </Button>
            </div>
          </Form>
        </Card>

        <Card className="!rounded-2xl !border-0 shadow-[0_10px_24px_rgba(15,23,42,0.06)]">
          <Title level={5} className="!mt-0">
            Đổi mật khẩu
          </Title>

          <Form<PasswordFormValues>
            form={passwordForm}
            layout="vertical"
            onFinish={onChangePassword}
            autoComplete="off"
          >
            <Form.Item
              label="Mật khẩu hiện tại"
              name="currentPassword"
              rules={[{ required: true, message: "Vui lòng nhập mật khẩu hiện tại" }]}
            >
              <Input.Password placeholder="Nhập mật khẩu hiện tại" />
            </Form.Item>

            <Form.Item
              label="Mật khẩu mới"
              name="newPassword"
              rules={[
                { required: true, message: "Vui lòng nhập mật khẩu mới" },
                { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự" },
              ]}
            >
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

            <div className="flex justify-end">
              <Button type="primary" htmlType="submit" loading={updatingPassword}>
                Cập nhật mật khẩu
              </Button>
            </div>
          </Form>
        </Card>
      </div>
    </MainLayout>
  )
}

export default ProfilePage
