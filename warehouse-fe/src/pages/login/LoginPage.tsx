import { Button, Card, Checkbox, Form, Input, Typography, message } from "antd"
import {
  LockOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from "@ant-design/icons"
import { Navigate, useNavigate } from "react-router-dom"
import { login } from "../../services/auth"
import { getAuthToken } from "../../utils/auth"

type LoginFormValues = {
  username: string
  password: string
}

const { Text, Title } = Typography

const LoginPage = () => {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const token = getAuthToken()

  if (token) {
    return <Navigate to="/dashboard" replace />
  }

  const handleLogin = async (values: LoginFormValues) => {
    try {
      await login(values)
      messageApi.success("Login successful")
      navigate("/dashboard", { replace: true })
    } catch {
      messageApi.error("Username or password is incorrect")
    }
  }

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center px-4">
      {contextHolder}

      <div className="pointer-events-none absolute -left-24 top-4 h-80 w-80 rounded-full bg-fuchsia-300/45 blur-3xl" />
      <div className="pointer-events-none absolute bottom-0 right-0 h-[30rem] w-[30rem] rounded-full bg-cyan-300/55 blur-3xl" />
      <div className="pointer-events-none absolute right-1/3 top-1/4 h-72 w-72 rounded-full bg-amber-200/60 blur-3xl" />

      <div className="relative mx-auto flex min-h-screen w-full max-w-7xl items-center px-4 py-8 sm:px-6 lg:px-8">
        <div className="grid w-full gap-8 lg:grid-cols-2 lg:gap-10">
          <section className="rounded-3xl border border-white/60 bg-white/70 p-8 text-slate-900 shadow-[0_20px_60px_rgba(14,116,144,0.2)] backdrop-blur-md sm:p-10">
            <Text className="!text-xs uppercase tracking-[0.2em] !text-cyan-700">
              Warehouse management system
            </Text>
            <Title className="!mb-4 !mt-4 !text-4xl !font-semibold !leading-tight !text-slate-900 sm:!text-5xl">
              Van hanh kho tap trung, chinh xac theo thoi gian thuc.
            </Title>
            <Text className="!text-base !leading-7 !text-slate-600">
              Theo doi ton kho, lo hang va luong nhap xuat trong mot dashboard
              thong nhat de doi van hanh xu ly nhanh hon moi ngay.
            </Text>

            <div className="mt-8 grid gap-4 sm:grid-cols-2">
              <div className="rounded-2xl border border-cyan-100 bg-cyan-50/80 p-4">
                <p className="text-sm text-slate-600">Do chinh xac ton kho</p>
                <p className="mt-1 text-2xl font-semibold text-cyan-700">99.9%</p>
              </div>
              <div className="rounded-2xl border border-amber-100 bg-amber-50/90 p-4">
                <p className="text-sm text-slate-600">Don xu ly moi ngay</p>
                <p className="mt-1 text-2xl font-semibold text-amber-600">12.5K+</p>
              </div>
            </div>

            <div className="mt-8 flex items-start gap-3 rounded-2xl border border-emerald-100 bg-emerald-50/90 p-4">
              <SafetyCertificateOutlined className="mt-1 text-lg text-emerald-600" />
              <Text className="!text-slate-700">
                Bao mat dang nhap va quyen truy cap theo vai tro giup du lieu kho
                luon duoc kiem soat an toan.
              </Text>
            </div>
          </section>

          <section className="flex items-center">
            <Card
              className="w-full rounded-3xl border border-white/70 bg-white/95 p-6 shadow-[0_20px_60px_rgba(30,41,59,0.2)] sm:p-8"
              bordered={false}
            >
              <div className="mb-8">
                <Text className="text-[11px] uppercase tracking-[0.16em] text-slate-500">
                  Chao mung quay lai
                </Text>
                <Title level={3} className="!mb-0 !mt-2 text-slate-900">
                  Dang nhap he thong
                </Title>
                <Text className="text-slate-500">
                  Nhap tai khoan de tiep tuc vao dashboard quan ly kho.
                </Text>
              </div>

              <Form layout="vertical" onFinish={handleLogin} autoComplete="off">
                <Form.Item
                  label="Username"
                  name="username"
                  rules={[{ required: true, message: "Please enter username" }]}
                >
                  <Input
                    prefix={<UserOutlined className="text-slate-400" />}
                    placeholder="Enter username"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  label="Password"
                  name="password"
                  rules={[{ required: true, message: "Please enter password" }]}
                >
                  <Input.Password
                    prefix={<LockOutlined className="text-slate-400" />}
                    placeholder="Enter password"
                    size="large"
                  />
                </Form.Item>

                <Form.Item>
                  <div className="flex items-center justify-between">
                    <Form.Item name="remember" valuePropName="checked" noStyle>
                      <Checkbox>Remember me</Checkbox>
                    </Form.Item>
                    <a
                      className="text-sm font-medium text-indigo-600 hover:text-indigo-500"
                      href=""
                    >
                      Forgot password
                    </a>
                  </div>
                </Form.Item>

                <Form.Item className="!mb-0 mt-6">
                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    size="large"
                    className="!h-11 !rounded-xl bg-indigo-600 hover:!bg-indigo-700"
                  >
                    Login
                  </Button>
                </Form.Item>
              </Form>
            </Card>
          </section>
        </div>
      </div>
    </div>
  )
}

export default LoginPage