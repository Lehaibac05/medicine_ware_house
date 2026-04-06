import { Layout } from "antd";
import SidebarNav from "./SidebarNav";
import TopBar from "./TopBar";
import ChatbotWidget from "../components/common/ChatbotWidget";
const { Content, Sider } = Layout;

function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <Layout className="min-h-screen bg-slate-100">
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen transition-all duration-300 z-30 !overflow-visible"
      >
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed top-0 z-20 right-0 left-[260px]">
          <TopBar />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          {children}
        </Content>
      </Layout>
      <ChatbotWidget />
    </Layout>
  );
}

export default MainLayout;
