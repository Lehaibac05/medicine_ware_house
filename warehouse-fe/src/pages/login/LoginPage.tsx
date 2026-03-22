import {
  Button,
  Checkbox,
  Form,
  Input,
  Typography,
  message,
} from "antd";
import {
  ArrowRightOutlined,
  LockOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Navigate, useNavigate } from "react-router-dom";
import { login } from "../../services/auth";
import { getAuthToken, setAuthToken } from "../../utils/auth";

type LoginFormValues = {
  username: string;
  password: string;
};

const { Text, Title } = Typography;

const LoginPage = () => {
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const token = getAuthToken();

  if (token) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleLogin = async (values: LoginFormValues) => {
    try {
      const response = await login(values);
      setAuthToken(response.token);
      messageApi.success("Login successful");
      navigate("/dashboard", { replace: true });
    } catch {
      messageApi.error("Tên đăng nhập hoặc mật khẩu không đúng");
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-[#f6fbf5] font-['Plus_Jakarta_Sans',system-ui,sans-serif] text-slate-900">
      {contextHolder}

      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(177,232,191,0.42),_transparent_32%),radial-gradient(circle_at_bottom_right,_rgba(220,244,192,0.44),_transparent_30%),linear-gradient(180deg,_#f7fcf5_0%,_#eef8ec_48%,_#f7fbf4_100%)]" />
      <div className="pointer-events-none absolute left-[-7rem] top-20 h-72 w-72 rounded-full bg-[#cfeecf]/80 blur-3xl" />
      <div className="pointer-events-none absolute right-[-6rem] top-10 h-80 w-80 rounded-full bg-[#e3f6bf]/80 blur-3xl" />
      <div className="pointer-events-none absolute bottom-0 right-1/4 h-64 w-64 rounded-full bg-[#d9f5df]/70 blur-3xl" />

      <div className="relative mx-auto flex min-h-screen w-full max-w-7xl items-center justify-center px-4 py-8 sm:px-6 lg:px-8">
        <section className="w-full max-w-md">
          <div className="rounded-[32px] border border-white/70 bg-white/88 p-6 shadow-[0_30px_70px_rgba(32,77,44,0.14)] backdrop-blur sm:p-8">
            <div className="mb-8 text-center">
              <Text className="mt-2 block text-[11px] uppercase tracking-[0.2em] text-emerald-800/60">
                Welcome back
              </Text>
              <Title
                level={3}
                className="!mb-0 !mt-2 !font-['Space_Grotesk',sans-serif] !text-slate-950"
              >
                Đăng nhập hệ thống
              </Title>
              <Text className="mt-2 block text-slate-500">
                Nhập tài khoản để tiếp tục vào dashboard quản lý kho thuốc.
              </Text>
            </div>

            <Form layout="vertical" onFinish={handleLogin} autoComplete="off">
              <Form.Item
                label={
                  <span className="font-medium text-slate-700">Tên đăng nhập</span>
                }
                name="username"
                rules={[{ required: true, message: "Vui lòng nhập tên đăng nhập" }]}
              >
                <Input
                  prefix={<UserOutlined className="text-emerald-800/45" />}
                  placeholder="Nhập tên đăng nhập..."
                  size="large"
                  className="!h-12 !rounded-2xl !border-emerald-900/10 !bg-[#f8fcf6] hover:!border-emerald-400 focus:!border-emerald-500"
                />
              </Form.Item>

              <Form.Item
                label={
                  <span className="font-medium text-slate-700">Mật khẩu</span>
                }
                name="password"
                rules={[{ required: true, message: "Vui lòng nhập mật khẩu" }]}
              >
                <Input.Password
                  prefix={<LockOutlined className="text-emerald-800/45" />}
                  placeholder="Nhập mật khẩu..."
                  size="large"
                  className="!h-12 !rounded-2xl !border-emerald-900/10 !bg-[#f8fcf6] hover:!border-emerald-400 focus:!border-emerald-500"
                />
              </Form.Item>

              <Form.Item>
                <div className="flex items-center justify-between gap-4">
                  <Form.Item name="remember" valuePropName="checked" noStyle>
                    <Checkbox>Ghi nhớ đăng nhập</Checkbox>
                  </Form.Item>
                  <a
                    className="text-sm font-medium text-emerald-800 transition hover:text-emerald-950"
                    href=""
                  >
                    Quên mật khẩu
                  </a>
                </div>
              </Form.Item>

              <Form.Item className="!mb-0 mt-6">
                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  size="large"
                  icon={<ArrowRightOutlined />}
                  iconPosition="end"
                  className="!h-12 !rounded-2xl !border-0 !bg-emerald-950 !text-base !font-semibold !shadow-[0_16px_32px_rgba(6,78,59,0.16)] hover:!bg-emerald-900"
                >
                  Đăng nhập
                </Button>
              </Form.Item>
            </Form>
          </div>
        </section>
      </div>
    </div>
  );
};

export default LoginPage;
