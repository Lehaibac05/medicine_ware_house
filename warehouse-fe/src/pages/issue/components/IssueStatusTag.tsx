import { Tag } from "antd"
import type { IssueStatus } from "../../../services/issue"

type Props = {
  status: IssueStatus | string
}

export default function IssueStatusTag({ status }: Props) {
  const normalized = String(status || "").toUpperCase()
  if (normalized === "PENDING") return <Tag color="gold">CHỜ DUYỆT</Tag>
  if (normalized === "APPROVED") return <Tag color="blue">ĐÃ DUYỆT</Tag>
  if (normalized === "COMPLETED") return <Tag color="green">ĐÃ HOÀN THÀNH</Tag>
  if (normalized === "REJECTED") return <Tag color="red">ĐÃ TỪ CHỐI</Tag>
  return <Tag>{normalized}</Tag>
}
