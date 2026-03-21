import { Tag } from "antd"
import type { IssueStatus } from "../../../services/issue"

type Props = {
  status: IssueStatus | string
}

export default function IssueStatusTag({ status }: Props) {
  const normalized = String(status || "").toUpperCase()
  if (normalized === "PENDING") return <Tag color="gold">PENDING</Tag>
  if (normalized === "APPROVED") return <Tag color="blue">APPROVED</Tag>
  if (normalized === "COMPLETED") return <Tag color="green">COMPLETED</Tag>
  if (normalized === "REJECTED") return <Tag color="red">REJECTED</Tag>
  return <Tag>{normalized}</Tag>
}
