import { Button, Flex, Input, Space, Typography, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";

const { Text } = Typography;

type UserRow = {
  key: string;
  userId: string;
  username: string;
  fullname: string;
  email: string;
  role: string;
  status: "Active" | "Inactive";
  createdDate: string;
};

const data: UserRow[] = [
  {
    key: "USR-001",
    userId: "#USR-001",
    username: "admin01",
    fullname: "Admin User",
    email: "admin@pharmacy.com",
    role: "Admin",
    status: "Active",
    createdDate: "2023-10-27",
  },
  {
    key: "USR-002",
    userId: "#USR-002",
    username: "john_doe",
    fullname: "John Doe",
    email: "john@example.com",
    role: "Staff",
    status: "Active",
    createdDate: "2023-10-25",
  },
  {
    key: "USR-003",
    userId: "#USR-003",
    username: "mary_smith",
    fullname: "Mary Smith",
    email: "mary@example.com",
    role: "Staff",
    status: "Inactive",
    createdDate: "2023-10-20",
  },
];

const columns: ColumnsType<UserRow> = [
  {
    title: "User ID",
    dataIndex: "userId",
    key: "userId",
    render: (value: string) => <Text strong>{value}</Text>,
    width: 120,
  },
  {
    title: "Username",
    dataIndex: "username",
    key: "username",
    width: 160,
  },
  {
    title: "Fullname",
    dataIndex: "fullname",
    key: "fullname",
    width: 160,
  },
  {
    title: "Email",
    dataIndex: "email",
    key: "email",
    width: 200,
  },
  {
    title: "Role",
    dataIndex: "role",
    key: "role",
    width: 120,
  },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    width: 120,
    render: (value: UserRow["status"]) => {
      if (value === "Active") return <Tag color="green">Active</Tag>;
      return <Tag color="red">Inactive</Tag>;
    },
  },
  {
    title: "Created Date",
    dataIndex: "createdDate",
    key: "createdDate",
    width: 160,
  },
  {
    title: "Actions",
    key: "actions",
    width: 160,
    render: () => (
      <Space>
        <Button size="small">View</Button>
        <Button size="small">Export</Button>
      </Space>
    ),
  },
];

function UserTable() {
  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Users list
        </Text>
      </div>
      <div className="w-[300px]">
        <Input.Search
          placeholder="Search by user ID..."
          className="w-[320px]"
          allowClear
        />
      </div>
    </Flex>
  );

  return (
    <BaseTable
      title={() => tableHeader}
      columns={columns}
      dataSource={data}
      scroll={{ x: 1100 }}
      cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
    />
  );
}

export default UserTable;
