import { Avatar, Layout, Typography, Dropdown, Tag } from "antd";
import { UserOutlined, LogoutOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import { clearAuthToken, getPrimaryRole, getRoleLabel, getUserName } from "../utils/auth";

const { Header } = Layout;
const { Text } = Typography;

function TopBar() {
  const navigate = useNavigate();
  const userName = getUserName()
  const roleLabel = getRoleLabel(getPrimaryRole());

  const handleMenuClick = ({ key }: { key: string }) => {
    if (key === "profile") {
      navigate("/profile")
      return
    }

    if (key === "logout") {
      handleLogout()
    }
  }

  const handleLogout = () => {
    clearAuthToken();
    navigate("/login", { replace: true });
  };

  return (
    <Header
      className="!h-[80px] px-6 flex items-center justify-end shadow-sm"
      style={{ background: "#ecfdf5" }} // pastel green
    >
      <div className="flex items-center gap-4">
        {/* ROLE */}
        <Tag
          className="!m-0 px-3 py-[2px] rounded-full text-xs font-medium border-0"
          style={{
            background: "#bbf7d0",
            color: "#14532d",
          }}
        >
          {roleLabel}
        </Tag>

        {/* PROFILE */}
        <Dropdown
          placement="bottomRight"
          arrow
          trigger={["click"]}
          menu={{
            onClick: handleMenuClick,
            items: [
              { key: "profile", label: "Thông tin cá nhân", icon: <UserOutlined /> },
              {
                key: "logout",
                label: "Đăng xuất",
                danger: true,
                icon: <LogoutOutlined />,
              },
            ],
          }}
        >
          <div className="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-green-100 transition cursor-pointer">
            <Avatar
              size={36}
              style={{
                background: "linear-gradient(135deg, #4ade80, #16a34a)",
              }}
              className="shadow-sm"
            >
              SD
            </Avatar>

            <div className="leading-tight">
              <Text className="block text-[11px] text-green-800/60">
                Xin chào,
              </Text>

              <div className="text-sm font-semibold text-green-900">
                {userName || "User"}
              </div>
            </div>
          </div>
        </Dropdown>
      </div>
    </Header>
  );
}

export default TopBar;
