import {
  AlertOutlined as AlertIcon,
  AppstoreOutlined,
  BellOutlined,
  LeftOutlined,
  ClockCircleOutlined as ClockIcon,
  DashboardOutlined,
  MenuOutlined,
  SearchOutlined,
  UserOutlined,
} from "@ant-design/icons"
import { useMemo, useState, type ReactNode } from "react"
import { Link, useLocation } from "react-router-dom"

type ERPLayoutProps = {
  title: string
  subtitle?: string
  children: ReactNode
}

type NavItem = {
  to: string
  label: string
  icon: ReactNode
  activeCheck?: (path: string) => boolean
}

const navItems: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", icon: <DashboardOutlined /> },
  {
    to: "/inventory",
    label: "Inventory",
    icon: <AppstoreOutlined />,
    activeCheck: (path) => path.startsWith("/inventory"),
  },
  {
    to: "/inventory?status=LOW_STOCK",
    label: "Low Stock",
    icon: <AlertIcon />,
    activeCheck: (path) => path.startsWith("/inventory"),
  },
  {
    to: "/inventory?status=EXPIRING_SOON",
    label: "Expiring Medicines",
    icon: <ClockIcon />,
    activeCheck: (path) => path.startsWith("/inventory"),
  },
  { to: "/alerts", label: "Alerts", icon: <BellOutlined /> },
]

export default function ERPLayout({ title, subtitle = "Warehouse ERP", children }: ERPLayoutProps) {
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  const activePath = useMemo(() => location.pathname, [location.pathname])

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-72 border-r border-slate-200 bg-white p-5 shadow-sm transition-transform lg:translate-x-0 ${mobileNavOpen ? "translate-x-0" : "-translate-x-full"}`}
      >
        <div className="mb-8 flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Pharma</p>
            <h1 className="text-2xl font-black tracking-tight">Warehouse ERP</h1>
          </div>
          <button
            className="rounded-lg p-1 text-slate-600 hover:bg-slate-100 lg:hidden"
            onClick={() => setMobileNavOpen(false)}
          >
            <LeftOutlined />
          </button>
        </div>

        <nav className="space-y-1">
          {navItems.map((item) => {
            const active = item.activeCheck ? item.activeCheck(activePath) : activePath === item.to
            return (
              <Link
                key={item.to}
                to={item.to}
                onClick={() => setMobileNavOpen(false)}
                className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition ${
                  active
                    ? "bg-slate-900 text-white"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                }`}
              >
                {item.icon}
                {item.label}
              </Link>
            )
          })}
        </nav>
      </aside>

      {mobileNavOpen ? (
        <button
          aria-label="Close menu"
          onClick={() => setMobileNavOpen(false)}
          className="fixed inset-0 z-30 bg-slate-900/30 lg:hidden"
        />
      ) : null}

      <div className="lg:ml-72">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 px-4 py-3 backdrop-blur sm:px-6">
          <div className="flex flex-wrap items-center gap-3">
            <button
              className="rounded-lg border border-slate-200 p-2 text-slate-700 hover:bg-slate-100 lg:hidden"
              onClick={() => setMobileNavOpen(true)}
            >
              <MenuOutlined />
            </button>

            <div className="min-w-[180px] flex-1">
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{subtitle}</p>
              <h2 className="text-lg font-bold sm:text-2xl">{title}</h2>
            </div>

            <div className="flex w-full items-center gap-3 sm:w-auto">
              <div className="flex h-10 flex-1 items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 sm:w-72 sm:flex-none">
                <SearchOutlined className="text-slate-500" />
                <input
                  className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400"
                  placeholder="Search medicines, lots, alerts..."
                />
              </div>

              <button className="relative rounded-xl border border-slate-200 bg-white p-2.5 text-slate-700 hover:bg-slate-100">
                <BellOutlined />
                <span className="absolute right-1 top-1 h-2.5 w-2.5 rounded-full bg-rose-500" />
              </button>

              <button className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-2.5 py-2 text-slate-700 hover:bg-slate-100">
                <UserOutlined />
                <span className="hidden text-sm font-semibold sm:inline">Admin</span>
              </button>
            </div>
          </div>
        </header>

        <main className="p-4 sm:p-6">{children}</main>
      </div>
    </div>
  )
}
