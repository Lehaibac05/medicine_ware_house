import {
  ArrowRightOutlined,
  AuditOutlined,
  BarcodeOutlined,
  CheckCircleFilled,
  ClockCircleOutlined,
  LoginOutlined,
  RadarChartOutlined,
  RobotOutlined,
  SafetyOutlined,
} from "@ant-design/icons";
import { Button, Image } from "antd";
import { Link } from "react-router-dom";
import { getAuthToken } from "../../utils/auth";
import logo from "../../assets/pharmacy_logo.png";

const featureCards = [
  {
    icon: <BarcodeOutlined className="text-2xl text-emerald-950" />,
    iconClassName: "bg-[#d7f5df]",
    title: "Kiểm soát lô thuốc chính xác",
    description:
      "Theo dõi số lượng, hạn dùng và vị trí từng lô trong một giao diện gọn, trực quan và dễ thao tác cho đội vận hành.",
  },
  {
    icon: <RobotOutlined className="text-2xl text-emerald-950" />,
    iconClassName: "bg-[#c8f2d2]",
    title: "AI gợi ý nhập hàng",
    description:
      "Phân tích tốc độ tiêu thụ, tồn kho an toàn và xu hướng sử dụng để đề xuất kế hoạch nhập thuốc hợp lý hơn.",
  },
  {
    icon: <ClockCircleOutlined className="text-2xl text-emerald-950" />,
    iconClassName: "bg-[#e2f7d5]",
    title: "Cảnh báo cận date tức thì",
    description:
      "Ưu tiên các lô sắp hết hạn và phát hiện mặt hàng chậm luân chuyển trước khi phát sinh thất thoát.",
  },
  {
    icon: <SafetyOutlined className="text-2xl text-emerald-950" />,
    iconClassName: "bg-[#d9efe0]",
    title: "Chuẩn hóa kiểm soát nội bộ",
    description:
      "Lưu vết thao tác, phân quyền theo vai trò và hỗ trợ đối soát để quy trình kho thuốc rõ ràng, an toàn hơn.",
  },
  {
    icon: <RadarChartOutlined className="text-2xl text-emerald-950" />,
    iconClassName: "bg-[#d4f4e6]",
    title: "Dashboard vận hành hiện đại",
    description:
      "Tổng hợp nhập xuất tồn, giá trị hàng hóa và tín hiệu rủi ro theo thời gian thực cho quyết định nhanh hơn.",
  },
  {
    icon: <AuditOutlined className="text-2xl text-emerald-950" />,
    iconClassName: "bg-[#e8f6dd]",
    title: "Truy xuất và kiểm kê liền mạch",
    description:
      "Hỗ trợ kiểm đếm nhanh, truy vết lịch sử lô thuốc và giảm thao tác thủ công trong các đợt rà soát kho.",
  },
];

const workflowSteps = [
  "Thu thập dữ liệu nhập xuất, tồn kho, hạn dùng và lịch sử tiêu thụ theo thời gian thực.",
  "AI phát hiện biến động nhu cầu, tín hiệu chậm luân chuyển và nguy cơ thiếu thuốc theo từng nhóm hàng.",
  "Hệ thống đề xuất cảnh báo ưu tiên, kế hoạch nhập hàng và hành động xử lý ngay trên dashboard.",
];

const LandingPage = () => {
  const isAuthenticated = Boolean(getAuthToken());
  const primaryHref = isAuthenticated ? "/dashboard" : "/login";
  const primaryLabel = isAuthenticated ? "Vào dashboard" : "Đăng nhập";

  return (
    <div className="min-h-screen bg-[#f6fbf5] font-['Plus_Jakarta_Sans',system-ui,sans-serif] text-slate-900">
      <div className="relative w-full overflow-hidden">
        {/* Background */}
        <div className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(circle_at_top_left,_rgba(177,232,191,0.42),_transparent_32%),radial-gradient(circle_at_top_right,_rgba(210,244,198,0.48),_transparent_28%),linear-gradient(180deg,_#f7fcf5_0%,_#eef8ec_48%,_#f7fbf4_100%)]" />
        <div className="pointer-events-none absolute left-[-8rem] top-32 z-0 h-72 w-72 rounded-full bg-[#cfeecf]/70 blur-3xl" />
        <div className="pointer-events-none absolute right-[-6rem] top-20 z-0 h-80 w-80 rounded-full bg-[#e3f6bf]/80 blur-3xl" />
      </div>
      
      {/* Navbar */}
      <div className="sticky top-0 z-50 border-b border-emerald-900/8 bg-white/70 backdrop-blur-xl supports-[backdrop-filter]:bg-white/60">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-3">
            <Image
              src={logo}
              preview={false}
              width={32}
              height={32}
              style={{ objectFit: "contain", display: "block" }}
            />
            <span className="leading-none font-['Space_Grotesk',sans-serif] text-xl font-bold tracking-tight text-emerald-950">
              Pharmacy
            </span>
          </Link>

          <Link to={primaryHref}>
            <Button
              type="primary"
              icon={<LoginOutlined />}
              className="!h-11 !rounded-full !border-0 !bg-emerald-950 !px-5 !font-semibold !shadow-[0_14px_32px_rgba(6,78,59,0.18)] hover:!bg-emerald-900"
            >
              {primaryLabel}
            </Button>
          </Link>
        </div>
      </div>

      {/* Hero */}
      <section className="relative z-10 mx-auto max-w-7xl px-4 pb-20 pt-12 sm:px-6 sm:pb-24 sm:pt-16 lg:px-8">
        <div className="max-w-3xl mx-auto text-center">
          <h1 className="mt-6 font-['Space_Grotesk',sans-serif] text-4xl font-extrabold leading-tight tracking-[-0.04em] text-slate-950 sm:text-6xl">
            <span className="text-emerald-500">Quản lý kho thuốc</span> đơn
            giản, rõ ràng và hiệu quả.
          </h1>

          {/* Description */}
          <p className="mt-5 text-lg leading-8 text-slate-700 sm:text-xl">
            Theo dõi tồn kho, cảnh báo cận date và nhận đề xuất từ AI trong một
            giao diện nhẹ và dễ dùng.
          </p>

          {/* Actions */}
          <div className="mt-8 flex justify-center flex-col gap-4 sm:flex-row sm:items-center">
            <Link to={primaryHref}>
              <Button
                type="primary"
                size="large"
                icon={<ArrowRightOutlined />}
                iconPosition="end"
                className="!h-14 !rounded-full !border-0 !bg-emerald-950 !px-8 !text-base !font-semibold !shadow-[0_18px_40px_rgba(6,78,59,0.18)] hover:!bg-emerald-900"
              >
                Bắt đầu
              </Button>
            </Link>

            <a href="#features">
              <Button
                size="large"
                className="!h-14 !rounded-full !border-emerald-900/10 !bg-white/85 !px-8 !text-base !font-semibold !text-emerald-950 !shadow-[0_12px_28px_rgba(15,23,42,0.08)] hover:!border-emerald-900/20 hover:!bg-white"
              >
                Tìm hiểu thêm
              </Button>
            </a>
          </div>

          {/* Features */}
          <div className="mt-8 flex flex-wrap justify-center gap-x-6 gap-y-3 text-sm text-slate-700">
            <div className="flex items-center gap-2">
              <CheckCircleFilled className="text-emerald-500" />
              Theo dõi tồn kho
            </div>
            <div className="flex items-center gap-2">
              <CheckCircleFilled className="text-emerald-500" />
              Cảnh báo hết hạn
            </div>
            <div className="flex items-center gap-2">
              <CheckCircleFilled className="text-emerald-500" />
              Gợi ý từ AI
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="bg-[#ffffff] py-20 sm:py-24">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="mx-auto max-w-3xl text-center">
            <p className="text-sm font-semibold uppercase tracking-[0.32em] text-emerald-700">
              Features
            </p>

            <h2 className="mt-4 font-['Space_Grotesk',sans-serif] text-3xl font-bold tracking-tight text-slate-950 sm:text-5xl">
              Quản lý kho thuốc dễ dàng và chính xác hơn
            </h2>

            <p className="mt-5 text-lg leading-8 text-slate-600">
              Theo dõi tồn kho, kiểm soát hạn dùng và nhận đề xuất từ AI trong
              một hệ thống rõ ràng, dễ sử dụng.
            </p>
          </div>

          {/* Feature Cards */}
          <div className="mt-12 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {featureCards.map((feature) => (
              <article
                key={feature.title}
                className="group flex flex-col gap-4 rounded-[30px] border border-emerald-900/8 bg-white/90 p-7 shadow-[0_16px_44px_rgba(32,77,44,0.06)] transition duration-200 hover:-translate-y-1.5 hover:shadow-[0_24px_54px_rgba(74,138,88,0.12)]"
              >
                <div
                  className={`flex h-14 w-14 items-center justify-center rounded-2xl border border-emerald-900/6 ${feature.iconClassName}`}
                >
                  {feature.icon}
                </div>

                <h3 className="font-['Space_Grotesk',sans-serif] text-xl font-bold tracking-tight text-slate-950">
                  {feature.title}
                </h3>

                <p className="text-sm leading-7 text-slate-600">
                  {feature.description}
                </p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section id="workflow" className="bg-[#f9fcf7] py-20 sm:py-24">
        <div className="mx-auto grid max-w-7xl gap-10 px-4 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:px-8">
          {/* LEFT */}
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.32em] text-emerald-700">
              AI Workflow
            </p>

            <h2 className="mt-4 font-['Space_Grotesk',sans-serif] text-3xl font-bold tracking-tight text-slate-950 sm:text-5xl">
              Từ dữ liệu đến hành động
            </h2>

            <p className="mt-5 max-w-2xl text-lg leading-8 text-slate-600">
              AI phân tích dữ liệu kho và đưa ra các bước xử lý rõ ràng, giúp
              bạn ra quyết định nhanh và chính xác.
            </p>

            <div className="mt-10 space-y-4">
              {workflowSteps.map((step, index) => (
                <div
                  key={step}
                  className="flex items-start gap-4 rounded-[28px] border border-emerald-900/8 bg-white p-5 shadow-[0_12px_30px_rgba(32,77,44,0.05)]"
                >
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-emerald-950 text-sm font-bold text-white">
                    0{index + 1}
                  </div>
                  <p className="text-base leading-7 text-slate-700">{step}</p>
                </div>
              ))}
            </div>
          </div>

          {/* RIGHT */}
          <div className="relative overflow-hidden rounded-[34px] border border-emerald-900/8 bg-emerald-950 p-8 text-white shadow-[0_30px_70px_rgba(15,23,42,0.18)]">
            <div className="absolute -right-16 top-0 h-48 w-48 rounded-full bg-[#77d391]/25 blur-3xl" />
            <div className="absolute -left-20 bottom-0 h-56 w-56 rounded-full bg-[#d0f2a8]/16 blur-3xl" />

            <div className="relative">
              <p className="text-sm font-semibold uppercase tracking-[0.28em] text-emerald-100/70">
                Dashboard
              </p>

              <h3 className="mt-4 font-['Space_Grotesk',sans-serif] text-3xl font-bold leading-tight">
                Toàn bộ tín hiệu kho trong một màn hình
              </h3>

              <div className="mt-8 grid gap-4 sm:grid-cols-2">
                <div className="rounded-3xl border border-white/10 bg-white/6 p-5">
                  <p className="text-sm text-emerald-50/70">Thuốc cận date</p>
                  <p className="mt-2 font-['Space_Grotesk',sans-serif] text-4xl font-bold text-white">
                    128
                  </p>
                  <p className="mt-2 text-sm text-emerald-200">
                    Sắp theo mức độ ưu tiên
                  </p>
                </div>

                <div className="rounded-3xl border border-white/10 bg-white/6 p-5">
                  <p className="text-sm text-emerald-50/70">
                    Đề xuất nhập hàng
                  </p>
                  <p className="mt-2 font-['Space_Grotesk',sans-serif] text-4xl font-bold text-white">
                    24
                  </p>
                  <p className="mt-2 text-sm text-[#d7f8ad]">
                    Dựa trên dữ liệu tiêu thụ
                  </p>
                </div>
              </div>

              <div className="mt-6 rounded-3xl border border-white/10 bg-white/6 p-5">
                <p className="text-sm text-emerald-50/70">Tín hiệu hôm nay</p>

                <ul className="mt-4 space-y-3 text-sm text-slate-100">
                  <li className="flex items-start gap-3">
                    <CheckCircleFilled className="mt-1 text-[#d7f8ad]" />
                    Nguy cơ thiếu hàng trong 7 ngày tới.
                  </li>
                  <li className="flex items-start gap-3">
                    <CheckCircleFilled className="mt-1 text-[#d7f8ad]" />
                    Lô thuốc cần ưu tiên xuất trước.
                  </li>
                  <li className="flex items-start gap-3">
                    <CheckCircleFilled className="mt-1 text-[#d7f8ad]" />
                    Phát hiện tồn kho bất thường.
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="bg-[#eaf5e6] py-20 text-slate-950">
        <div className="mx-auto max-w-5xl px-4 text-center sm:px-6 lg:px-8">
          <p className="text-sm font-semibold uppercase tracking-[0.32em] text-emerald-700">
            Bắt đầu ngay
          </p>

          <h2 className="mt-4 font-['Space_Grotesk',sans-serif] text-3xl font-bold tracking-tight sm:text-5xl">
            Kiểm soát kho thuốc của bạn ngay hôm nay
          </h2>

          <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-slate-700">
            Theo dõi tồn kho, giảm rủi ro hết hạn và đưa ra quyết định nhanh hơn
            với sự hỗ trợ của AI.
          </p>

          <div className="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <Link to={primaryHref}>
              <Button
                type="primary"
                size="large"
                className="!h-14 !rounded-full !border-0 !bg-emerald-950 !px-8 !text-base !font-semibold hover:!bg-emerald-900"
              >
                {primaryLabel}
              </Button>
            </Link>

            {!isAuthenticated && (
              <Link to="/login">
                <Button
                  size="large"
                  className="!h-14 !rounded-full !border-emerald-900/10 !bg-white/85 !px-8 !text-base !font-semibold !text-emerald-950 hover:!border-emerald-900/20 hover:!bg-white"
                >
                  Xem demo
                </Button>
              </Link>
            )}
          </div>
        </div>
      </section>

      <footer className="border-t border-emerald-900/8 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="grid gap-8 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-4">
            {/* Col 1 - Brand */}
            <div>
              <p className="font-['Space_Grotesk',sans-serif] text-lg font-bold text-slate-950">
                Pharmacy
              </p>
              <p className="mt-2">
                Quản lý kho thuốc thông minh, đơn giản và hiệu quả.
              </p>
            </div>

            {/* Col 2 - Product */}
            <div>
              <p className="font-semibold text-slate-950">Sản phẩm</p>
              <ul className="mt-3 space-y-2">
                <li>
                  <a href="#features" className="hover:text-slate-950">
                    Tính năng
                  </a>
                </li>
                <li>
                  <a href="#workflow" className="hover:text-slate-950">
                    Workflow AI
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-950">
                    Bảng giá
                  </a>
                </li>
              </ul>
            </div>

            {/* Col 3 - Company */}
            <div>
              <p className="font-semibold text-slate-950">Công ty</p>
              <ul className="mt-3 space-y-2">
                <li>
                  <a href="#" className="hover:text-slate-950">
                    Giới thiệu
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-950">
                    Liên hệ
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-950">
                    Tuyển dụng
                  </a>
                </li>
              </ul>
            </div>

            {/* Col 4 - Contact */}
            <div>
              <p className="font-semibold text-slate-950">Liên hệ</p>
              <ul className="mt-3 space-y-2">
                <li>
                  <a
                    href="mailto:support@pharmaai.local"
                    className="hover:text-slate-950"
                  >
                    support@pharmaai.local
                  </a>
                </li>
                <li>1900 6868</li>
                <li className="text-xs text-slate-500 mt-2">© 2026 PharmaAI</li>
              </ul>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
