import { Button, DatePicker, Input, Layout, Select, Typography } from "antd";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import UserTable from "./components/UserTable";

const { Content, Sider } = Layout;
const { Text } = Typography;
const { RangePicker } = DatePicker;

const roleOptions = [
  { value: "all", label: "All roles" },
  { value: "admin", label: "Admin" },
  { value: "staff", label: "Staff" },
];

const statusOptions = [
  { value: "all", label: "All status" },
  { value: "active", label: "Active" },
  { value: "inactive", label: "Inactive" },
];

const UserPage = () => {
  return (
    <Layout className="h-screen bg-slate-100">
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="Users" subtitle="Management" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]">
                Apply filters
              </Button>
            }
          >
            {/* Role */}
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Role</Text>
              <Select options={roleOptions} defaultValue="all" />
            </div>

            {/* Status */}
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Status</Text>
              <Select options={statusOptions} defaultValue="all" />
            </div>

            {/* Date */}
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Created Date</Text>
              <RangePicker
                className="w-full"
                format="YYYY-MM-DD"
                placeholder={["Start date", "End date"]}
              />
            </div>

            {/* User ID */}
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">User ID</Text>
              <Input placeholder="Enter user ID" />
            </div>
          </BaseFilterCard>

          <UserTable />
        </Content>
      </Layout>
    </Layout>
  );
};

export default UserPage;
