import { Layout } from "antd";
import { useState } from "react";
import SidebarNav from "./SidebarNav";
import TopBar from "./TopBar";
const { Content, Sider } = Layout;

function MainLayout({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout className="min-h-screen bg-slate-100">
      <Sider
        width={collapsed ? 100 : 260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen transition-all duration-300 z-30 !overflow-visible"
      >
        <SidebarNav collapsed={collapsed} setCollapsed={setCollapsed} />
      </Sider>

      <Layout
        className={`transition-all duration-300 ${
          collapsed ? "lg:ml-[100px]" : "lg:ml-[260px]"
        }`}
      >
        <div
          className={`fixed top-0 z-20 right-0 transition-all duration-300 ${
            collapsed ? "left-[100px]" : "left-[260px]"
          }`}
        >
          <TopBar />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          {children}
        </Content>
      </Layout>
    </Layout>
  );
}

export default MainLayout;
