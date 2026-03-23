import { Button, Result } from "antd"
import { Link } from "react-router-dom"

export default function NotFoundPage() {
  return (
    <Result
      status="404"
      title="404"
      subTitle="Bạn không thể truy cập trang này hoặc trang không tồn tại."
      extra={
        <Button type="primary">
          <Link to="/dashboard">Trở về bảng điều khiển</Link>
        </Button>
      }
    />
  )
}
