import {
  Alert,
  Button,
  Card,
  Input,
  InputNumber,
  Select,
  Spin,
  Switch,
  Tag,
  Typography,
  message,
} from "antd";
import { useEffect, useState, type ReactNode } from "react";
import {
  BellOutlined,
  DatabaseOutlined,
  LockOutlined,
  ReloadOutlined,
  RobotOutlined,
  SaveOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
} from "@ant-design/icons";
import MainLayout from "../../layouts/MainLayout";
import {
  getSettings,
  updateGeneralSettings,
  updateInventorySettings,
  updateNotificationSettings,
  updateSecuritySettings,
  updateSystemSettings,
} from "../../services/settings";

const { Text, Title } = Typography;

const panelClass =
  "!rounded-[28px] !border-slate-200 shadow-[0_18px_44px_rgba(15,23,42,0.06)]";

const Field = ({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) => (
  <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4">
    <Text className="block text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">
      {label}
    </Text>
    <div className="mt-2">{children}</div>
    {hint ? (
      <Text className="mt-2 block text-xs text-slate-500">{hint}</Text>
    ) : null}
  </div>
);

const ToggleCard = ({
  title,
  description,
  checked,
  onChange,
  disabled,
}: {
  title: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
}) => (
  <div className="flex items-start justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4">
    <div>
      <Text className="block text-sm font-semibold text-slate-800">
        {title}
      </Text>
      <Text className="text-xs text-slate-500">{description}</Text>
    </div>
    <Switch checked={checked} onChange={onChange} disabled={disabled} />
  </div>
);

const SectionTitle = ({
  icon,
  title,
  subtitle,
  color,
}: {
  icon: ReactNode;
  title: string;
  subtitle: string;
  color: string;
}) => (
  <div className="flex items-start gap-4">
    <div
      className={`flex h-11 w-11 items-center justify-center rounded-2xl text-lg ${color}`}
    >
      {icon}
    </div>
    <div>
      <Text className="block text-[11px] font-semibold uppercase tracking-[0.3em] text-slate-400">
        Control Module
      </Text>
      <Title level={4} className="!mb-1 !mt-1 !text-slate-900">
        {title}
      </Title>
      <Text className="text-sm text-slate-500">{subtitle}</Text>
    </div>
  </div>
);

const SettingsPage = () => {
  const [messageApi, contextHolder] = message.useMessage();
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  const [pharmacyName, setPharmacyName] = useState("Main Street Pharmacy");
  const [contactEmail, setContactEmail] = useState("admin@pharmacy.com");
  const [address, setAddress] = useState(
    "123 Health Ave, Medical District, City State",
  );
  const [phoneNumber, setPhoneNumber] = useState("+1 (555) 012-3456");
  const [lowStockThreshold, setLowStockThreshold] = useState(50);
  const [expiryAlertDays, setExpiryAlertDays] = useState(30);
  const [enableAIForecast, setEnableAIForecast] = useState(true);
  const [autoOrderEnabled, setAutoOrderEnabled] = useState(false);
  const [reorderPoint, setReorderPoint] = useState(20);
  const [passwordPolicy, setPasswordPolicy] = useState("medium");
  const [sessionTimeout, setSessionTimeout] = useState(15);
  const [twoFactorAuth, setTwoFactorAuth] = useState(false);
  const [loginAttempts, setLoginAttempts] = useState(5);
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [lowStockAlert, setLowStockAlert] = useState(true);
  const [expiryAlert, setExpiryAlert] = useState(true);
  const [orderAlert, setOrderAlert] = useState(true);
  const [darkMode, setDarkMode] = useState(false);
  const [language, setLanguage] = useState("en");
  const [timezone, setTimezone] = useState("UTC+7");
  const [dateFormat, setDateFormat] = useState("DD/MM/YYYY");

  useEffect(() => {
    const loadSettings = async () => {
      try {
        setLoading(true);
        const settings = await getSettings();
        setPharmacyName(settings.general.pharmacyName);
        setContactEmail(settings.general.contactEmail);
        setAddress(settings.general.address);
        setPhoneNumber(settings.general.phoneNumber);
        setLowStockThreshold(settings.inventory.lowStockThreshold);
        setExpiryAlertDays(settings.inventory.expiryAlertDays);
        setEnableAIForecast(settings.inventory.enableAIForecast);
        setAutoOrderEnabled(settings.inventory.autoOrderEnabled);
        setReorderPoint(settings.inventory.reorderPoint);
        setPasswordPolicy(settings.security.passwordPolicy);
        setSessionTimeout(settings.security.sessionTimeout);
        setTwoFactorAuth(settings.security.twoFactorAuth);
        setLoginAttempts(settings.security.loginAttempts);
        setEmailNotifications(settings.notifications.emailNotifications);
        setLowStockAlert(settings.notifications.lowStockAlert);
        setExpiryAlert(settings.notifications.expiryAlert);
        setOrderAlert(settings.notifications.orderAlert);
        setDarkMode(settings.system.darkMode);
        setLanguage(settings.system.language);
        setTimezone(settings.system.timezone);
        setDateFormat(settings.system.dateFormat);
      } catch (error) {
        console.error("Failed to load settings:", error);
        messageApi.warning("Using default settings");
      } finally {
        setLoading(false);
      }
    };

    loadSettings();
  }, [messageApi]);

  const withSaving = async (
    action: () => Promise<void>,
    successText: string,
  ) => {
    try {
      setSaving(true);
      await action();
      messageApi.success(successText);
    } catch {
      messageApi.error("Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  const automationScore = [
    enableAIForecast,
    autoOrderEnabled,
    twoFactorAuth,
    emailNotifications,
  ].filter(Boolean).length;

  return (
    <MainLayout>
      {contextHolder}
      {loading ? (
        <div className="flex min-h-[420px] items-center justify-center rounded-[32px] border border-white/70 bg-white/80">
          <Spin size="large" tip="Loading settings..." />
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-[32px] border border-cyan-100 bg-[linear-gradient(135deg,#f8fffe_0%,#eef9ff_52%,#f4fffb_100%)] p-6 text-slate-900 shadow-[0_24px_60px_rgba(15,23,42,0.08)] lg:p-8">
            <div className="grid gap-6 xl:grid-cols-[1.35fr_0.95fr]">
              <div>
                <div className="flex flex-wrap gap-3">
                  <Tag className="!m-0 rounded-full border-emerald-200 bg-emerald-50 px-4 py-1 text-[11px] font-semibold uppercase tracking-[0.3em] text-emerald-700">
                    AI Medicine Warehouse
                  </Tag>
                  <Tag className="!m-0 rounded-full border-cyan-200 bg-cyan-50 px-4 py-1 text-[11px] font-semibold uppercase tracking-[0.3em] text-cyan-700">
                    Smart Operations
                  </Tag>
                </div>
                <Title level={2} className="!mb-3 !mt-5 !text-slate-900">
                  Cài đặt hệ thống quản lí kho thuốc thông minh
                </Title>
                <Text className="max-w-3xl text-base text-slate-600">
                  Giao diện điều phối dành cho kho dược có AI dự báo nhu cầu,
                  cảnh báo cận hạn và kiểm soát nhập hàng.
                </Text>
                <div className="mt-6 grid gap-4 md:grid-cols-3">
                  <Card className="!rounded-[24px] !border-cyan-100 !bg-white/90 shadow-[0_12px_30px_rgba(14,165,233,0.08)]">
                    <RobotOutlined className="text-lg text-cyan-600" />
                    <Text className="mt-3 block text-xs uppercase tracking-[0.24em] text-slate-400">
                      AI Forecast
                    </Text>
                    <Title level={4} className="!mb-1 !mt-2 !text-slate-900">
                      {enableAIForecast ? "Đang kích hoạt" : "Tạm tắt"}
                    </Title>
                    <Text className="text-xs text-slate-500">
                      Dự báo biến động tiêu thụ thuốc.
                    </Text>
                  </Card>
                  <Card className="!rounded-[24px] !border-emerald-100 !bg-white/90 shadow-[0_12px_30px_rgba(16,185,129,0.08)]">
                    <DatabaseOutlined className="text-lg text-emerald-600" />
                    <Text className="mt-3 block text-xs uppercase tracking-[0.24em] text-slate-400">
                      Expiry Window
                    </Text>
                    <Title level={4} className="!mb-1 !mt-2 !text-slate-900">
                      {expiryAlertDays} ngày
                    </Title>
                    <Text className="text-xs text-slate-500">
                      Khoảng báo trước khi thuốc hết hạn.
                    </Text>
                  </Card>
                  <Card className="!rounded-[24px] !border-amber-100 !bg-white/90 shadow-[0_12px_30px_rgba(245,158,11,0.08)]">
                    <SafetyCertificateOutlined className="text-lg text-amber-600" />
                    <Text className="mt-3 block text-xs uppercase tracking-[0.24em] text-slate-400">
                      Automation
                    </Text>
                    <Title level={4} className="!mb-1 !mt-2 !text-slate-900">
                      {automationScore}/4 mô-đun
                    </Title>
                    <Text className="text-xs text-slate-500">
                      Mức độ tự động hóa đang dùng.
                    </Text>
                  </Card>
                </div>
              </div>

              <div className="rounded-[28px] border border-slate-200 bg-white/85 p-5 shadow-[0_14px_36px_rgba(15,23,42,0.06)] backdrop-blur">
                <Text className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">
                  Snapshot
                </Text>
                <Title level={4} className="!mb-0 !mt-4 !text-slate-900">
                  {pharmacyName}
                </Title>
                <Text className="text-sm text-slate-500">{contactEmail}</Text>
                <div className="mt-5 grid gap-3 sm:grid-cols-2">
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <Text className="block text-xs uppercase tracking-[0.2em] text-slate-400">
                      Reorder Point
                    </Text>
                    <Text className="text-lg font-semibold text-slate-900">
                      {reorderPoint} đơn vị
                    </Text>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <Text className="block text-xs uppercase tracking-[0.2em] text-slate-400">
                      Session Timeout
                    </Text>
                    <Text className="text-lg font-semibold text-slate-900">
                      {sessionTimeout} phút
                    </Text>
                  </div>
                </div>
                <Alert
                  className="!mt-4 !rounded-2xl !border-0"
                  type={autoOrderEnabled ? "success" : "warning"}
                  showIcon
                  message={
                    autoOrderEnabled
                      ? "Tự động đề xuất nhập hàng đang bật."
                      : "Hệ thống hiện chỉ cảnh báo, chưa tự động đặt hàng."
                  }
                />
              </div>
            </div>
          </div>

          <div className="grid gap-6 xl:grid-cols-2">
            <Card className={panelClass}>
              <SectionTitle
                icon={<SettingOutlined />}
                title="Thông tin cơ sở dược"
                subtitle="Định danh kho thuốc và đầu mối vận hành."
                color="bg-emerald-100 text-emerald-700"
              />
              <div className="mt-6 grid gap-4 md:grid-cols-2">
                <Field label="Tên nhà thuốc / kho">
                  <Input
                    value={pharmacyName}
                    onChange={(e) => setPharmacyName(e.target.value)}
                    size="large"
                  />
                </Field>
                <Field label="Email điều hành">
                  <Input
                    value={contactEmail}
                    onChange={(e) => setContactEmail(e.target.value)}
                    type="email"
                    size="large"
                  />
                </Field>
                <Field
                  label="Địa chỉ kho"
                  hint="Dùng cho điều phối nhập hàng và báo cáo nội bộ."
                >
                  <Input
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    size="large"
                  />
                </Field>
                <Field label="Số điện thoại trực vận hành">
                  <Input
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    size="large"
                  />
                </Field>
              </div>
              <div className="mt-6 flex justify-end">
                <Button
                  type="primary"
                  size="large"
                  icon={<SaveOutlined />}
                  loading={saving}
                  onClick={() =>
                    withSaving(
                      () =>
                        updateGeneralSettings({
                          pharmacyName,
                          contactEmail,
                          address,
                          phoneNumber,
                        }),
                      "General settings saved successfully",
                    )
                  }
                  className="!rounded-xl !bg-emerald-600 hover:!bg-emerald-500"
                >
                  Lưu thông tin cơ sở
                </Button>
              </div>
            </Card>

            <Card className={panelClass}>
              <SectionTitle
                icon={<DatabaseOutlined />}
                title="Chính sách tồn kho và AI"
                subtitle="Ngưỡng cảnh báo, tái nhập và dự báo thông minh."
                color="bg-cyan-100 text-cyan-700"
              />
              <div className="mt-6 grid gap-4">
                <Field
                  label="Ngưỡng tồn kho thấp"
                  hint="Cảnh báo khi thuốc xuống dưới mức này."
                >
                  <InputNumber
                    value={lowStockThreshold}
                    onChange={(v) => setLowStockThreshold(v || 50)}
                    min={0}
                    max={1000}
                    className="w-full"
                    size="large"
                    addonAfter="đơn vị"
                  />
                </Field>
                <div className="grid gap-4 md:grid-cols-2">
                  <Field label="Cảnh báo hết hạn">
                    <InputNumber
                      value={expiryAlertDays}
                      onChange={(v) => setExpiryAlertDays(v || 30)}
                      min={1}
                      max={365}
                      className="w-full"
                      size="large"
                      addonAfter="ngày"
                    />
                  </Field>
                  <Field label="Điểm tái nhập">
                    <InputNumber
                      value={reorderPoint}
                      onChange={(v) => setReorderPoint(v || 20)}
                      min={0}
                      max={1000}
                      className="w-full"
                      size="large"
                      addonAfter="đơn vị"
                    />
                  </Field>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <ToggleCard
                    title="AI dự báo nhu cầu"
                    description="Ước tính tiêu thụ thuốc theo dữ liệu lịch sử."
                    checked={enableAIForecast}
                    onChange={setEnableAIForecast}
                  />
                  <ToggleCard
                    title="Tự động đề xuất nhập hàng"
                    description="Sinh gợi ý mua khi có nguy cơ thiếu thuốc."
                    checked={autoOrderEnabled}
                    onChange={setAutoOrderEnabled}
                  />
                </div>
              </div>
              <div className="mt-6 flex justify-end">
                <Button
                  type="primary"
                  size="large"
                  icon={<SaveOutlined />}
                  loading={saving}
                  onClick={() =>
                    withSaving(
                      () =>
                        updateInventorySettings({
                          lowStockThreshold,
                          expiryAlertDays,
                          enableAIForecast,
                          autoOrderEnabled,
                          reorderPoint,
                        }),
                      "Inventory settings saved successfully",
                    )
                  }
                  className="!rounded-xl !bg-sky-600 hover:!bg-sky-500"
                >
                  Lưu chính sách tồn kho
                </Button>
              </div>
            </Card>

            <Card className={panelClass}>
              <SectionTitle
                icon={<LockOutlined />}
                title="Bảo mật và truy cập"
                subtitle="Kiểm soát đăng nhập cho tài khoản nội bộ."
                color="bg-amber-100 text-amber-700"
              />
              <div className="mt-6 grid gap-4">
                <Field label="Chính sách mật khẩu">
                  <Select
                    value={passwordPolicy}
                    onChange={setPasswordPolicy}
                    size="large"
                    options={[
                      { value: "low", label: "Low (6+ characters)" },
                      {
                        value: "medium",
                        label: "Medium (8+ chars, Alpha-numeric)",
                      },
                      {
                        value: "high",
                        label: "High (10+ chars, Special symbols)",
                      },
                      {
                        value: "strict",
                        label: "Strict (12+ chars, All types)",
                      },
                    ]}
                  />
                </Field>
                <div className="grid gap-4 md:grid-cols-2">
                  <Field label="Thời gian hết phiên">
                    <Select
                      value={sessionTimeout}
                      onChange={setSessionTimeout}
                      size="large"
                      options={[
                        { value: 5, label: "5 phút" },
                        { value: 15, label: "15 phút" },
                        { value: 30, label: "30 phút" },
                        { value: 60, label: "1 giờ" },
                        { value: 120, label: "2 giờ" },
                      ]}
                    />
                  </Field>
                  <Field
                    label="Số lần đăng nhập sai"
                    hint="Khoá tạm khi vượt ngưỡng."
                  >
                    <InputNumber
                      value={loginAttempts}
                      onChange={(v) => setLoginAttempts(v || 5)}
                      min={3}
                      max={10}
                      className="w-full"
                      size="large"
                      addonAfter="lần"
                    />
                  </Field>
                </div>
                <ToggleCard
                  title="Xác thực hai lớp (2FA)"
                  description="Bảo vệ tài khoản quản trị và phê duyệt nhập kho."
                  checked={twoFactorAuth}
                  onChange={setTwoFactorAuth}
                />
              </div>
              <div className="mt-6 flex justify-end">
                <Button
                  size="large"
                  icon={<SaveOutlined />}
                  loading={saving}
                  onClick={() =>
                    withSaving(
                      () =>
                        updateSecuritySettings({
                          passwordPolicy,
                          sessionTimeout,
                          twoFactorAuth,
                          loginAttempts,
                        }),
                      "Security settings saved successfully",
                    )
                  }
                  danger
                  className="!rounded-xl"
                >
                  Lưu chính sách bảo mật
                </Button>
              </div>
            </Card>

            <Card className={panelClass}>
              <SectionTitle
                icon={<BellOutlined />}
                title="Cảnh báo và tuỳ chọn hệ thống"
                subtitle="Kênh thông báo, múi giờ và định dạng dữ liệu."
                color="bg-slate-200 text-slate-700"
              />
              <div className="mt-6 grid gap-4">
                <ToggleCard
                  title="Thông báo email"
                  description="Gửi cảnh báo tới email điều hành."
                  checked={emailNotifications}
                  onChange={setEmailNotifications}
                />
                <div className="grid gap-4 md:grid-cols-3">
                  <ToggleCard
                    title="Tồn kho thấp"
                    description="Cảnh báo thiếu thuốc."
                    checked={lowStockAlert}
                    onChange={setLowStockAlert}
                    disabled={!emailNotifications}
                  />
                  <ToggleCard
                    title="Cận hạn"
                    description="Cảnh báo thuốc sắp hết date."
                    checked={expiryAlert}
                    onChange={setExpiryAlert}
                    disabled={!emailNotifications}
                  />
                  <ToggleCard
                    title="Đơn hàng"
                    description="Cập nhật biến động nhập kho."
                    checked={orderAlert}
                    onChange={setOrderAlert}
                    disabled={!emailNotifications}
                  />
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <Field label="Ngôn ngữ giao diện">
                    <Select
                      value={language}
                      onChange={setLanguage}
                      size="large"
                      options={[
                        { value: "en", label: "English" },
                        { value: "vi", label: "Tiếng Việt" },
                        { value: "es", label: "Español" },
                        { value: "fr", label: "Français" },
                      ]}
                    />
                  </Field>
                  <Field label="Múi giờ vận hành">
                    <Select
                      value={timezone}
                      onChange={setTimezone}
                      size="large"
                      options={[
                        { value: "UTC+7", label: "UTC+7 (Bangkok, Hanoi)" },
                        { value: "UTC+8", label: "UTC+8 (Singapore, Manila)" },
                        { value: "UTC+9", label: "UTC+9 (Tokyo, Seoul)" },
                        { value: "UTC-5", label: "UTC-5 (New York)" },
                        { value: "UTC+0", label: "UTC+0 (London)" },
                      ]}
                    />
                  </Field>
                </div>
                <Field label="Định dạng ngày">
                  <Select
                    value={dateFormat}
                    onChange={setDateFormat}
                    size="large"
                    options={[
                      { value: "DD/MM/YYYY", label: "DD/MM/YYYY (31/12/2026)" },
                      { value: "MM/DD/YYYY", label: "MM/DD/YYYY (12/31/2026)" },
                      { value: "YYYY-MM-DD", label: "YYYY-MM-DD (2026-12-31)" },
                    ]}
                  />
                </Field>
                <ToggleCard
                  title="Chế độ tối"
                  description="Tùy chọn giao diện dự phòng cho các phiên bản sau."
                  checked={darkMode}
                  onChange={setDarkMode}
                />
                <Alert
                  type="info"
                  showIcon
                  className="!rounded-2xl"
                  message={`Thông báo sẽ gửi tới ${contactEmail}. Dark mode hiện mới là tuỳ chọn dự phòng.`}
                />
              </div>
              <div className="mt-6 flex flex-wrap justify-end gap-3">
                <Button
                  type="primary"
                  size="large"
                  icon={<SaveOutlined />}
                  loading={saving}
                  onClick={() =>
                    withSaving(
                      () =>
                        updateNotificationSettings({
                          emailNotifications,
                          lowStockAlert,
                          expiryAlert,
                          orderAlert,
                        }),
                      "Notification settings saved successfully",
                    )
                  }
                  className="!rounded-xl !bg-emerald-600 hover:!bg-emerald-500"
                >
                  Lưu cảnh báo
                </Button>
                <Button
                  type="primary"
                  size="large"
                  icon={<SaveOutlined />}
                  loading={saving}
                  onClick={() =>
                    withSaving(
                      () =>
                        updateSystemSettings({
                          darkMode,
                          language,
                          timezone,
                          dateFormat,
                        }),
                      "System settings saved successfully",
                    )
                  }
                  className="!rounded-xl !bg-slate-900 hover:!bg-slate-800"
                >
                  Lưu tuỳ chọn
                </Button>
                <Button
                  size="large"
                  icon={<ReloadOutlined />}
                  onClick={() => window.location.reload()}
                  className="!rounded-xl"
                >
                  Tải lại
                </Button>
              </div>
            </Card>
          </div>
        </>
      )}
    </MainLayout>
  );
};

export default SettingsPage;
