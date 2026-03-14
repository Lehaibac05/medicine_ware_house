import { Avatar, Input, Layout, Space, Typography, Dropdown } from 'antd'
import { SearchOutlined, UserOutlined, LogoutOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { clearAuthToken } from '../utils/auth'

const { Header } = Layout
const { Text, Title } = Typography

type TopBarProps = {
  title?: string
  subtitle?: string
}

function TopBar({ title = 'Inventory', subtitle = 'Warehouse' }: TopBarProps) {
  const navigate = useNavigate()

  const handleLogout = () => {
    clearAuthToken()
    navigate('/login', { replace: true })
  }

  return (
    <Header className="!h-[90px] !bg-white px-6 py-4 border-b border-slate-200 flex flex-col md:flex-row items-center justify-between gap-6 shadow-sm">
      <div className="flex flex-col mb-4 md:mb-0">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          {subtitle}
        </Text>

        <Title level={3} className="!m-0">
          {title}
        </Title>
      </div>

      <div className="flex flex-1 w-full md:w-auto flex-col items-stretch gap-3 md:flex-row md:items-center md:justify-end">
        {/* <Input
          placeholder="Search medicines, batches, orders..."
          prefix={<SearchOutlined />}
          className="w-full md:max-w-sm rounded-full"
          allowClear
        /> */}

        <div className="flex items-center gap-3 bg-transparent px-3 py-0.5">
          <Dropdown menu={{ items: [
            { key: 'profile', label: 'Profile', icon: <UserOutlined /> },
            { key: 'logout', label: 'Logout', onClick: handleLogout, danger: true, icon: <LogoutOutlined /> }
          ] }} placement="bottomRight" arrow trigger={['click']}>
            <Space className="cursor-pointer">
              <Avatar size={36} className="!bg-slate-900">
                SD
              </Avatar>

              <div>
                <Text className="block text-[11px] text-slate-400">
                  Profile
                </Text>

                <div className="text-sm font-semibold text-slate-900">
                  Sam Duong
                </div>
              </div>
            </Space>
          </Dropdown>
        </div>
      </div>
    </Header>
  )
}

export default TopBar
