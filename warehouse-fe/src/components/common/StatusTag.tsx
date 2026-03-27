import { Tag } from "antd"

type StatusDomain = "invoice" | "goodsReceipt" | "purchaseOrder" | "payment"

type StatusTagProps = {
  status?: string | null
  domain: StatusDomain
}

const normalize = (value?: string | null) =>
  value?.trim().toUpperCase().replace(/[\s-]+/g, "_") ?? ""

const palette: Record<StatusDomain, Record<string, { color: string; label?: string }>> = {
  invoice: {
    PENDING_VERIFICATION: { color: "gold" },
    VERIFIED: { color: "blue" },
    REJECTED: { color: "red" },
    PARTIALLY_PAID: { color: "volcano" },
    PAID: { color: "green" },
  },
  goodsReceipt: {
    APPROVED: { color: "green", label: "ĐÃ DUYỆT" },
    PENDING_APPROVAL: { color: "gold", label: "CHỜ DUYỆT" },
    REJECTED: { color: "red", label: "ĐÃ TỪ CHỐI" },
  },
  purchaseOrder: {
    APPROVED: { color: "green", label: "ĐÃ DUYỆT" },
    RECEIVED: { color: "green", label: "ĐÃ NHẬN HÀNG" },
    SHIPPING: { color: "blue", label: "ĐANG GIAO HÀNG" },
    CONFIRMED: { color: "geekblue", label: "ĐÃ XÁC NHẬN" },
    PENDING: { color: "gold", label: "CHỜ XỬ LÝ" },
    REJECTED: { color: "red", label: "ĐÃ TỪ CHỐI" },
    CANCELLED: { color: "default", label: "ĐÃ HỦY" },
  },
  payment: {
    COMPLETED: { color: "green" },
    PENDING: { color: "gold" },
    CANCELLED: { color: "red" },
  },
}

export default function StatusTag({ status, domain }: StatusTagProps) {
  const normalized = normalize(status)
  const mapped = palette[domain][normalized]
  const label = mapped?.label ?? normalized ?? "UNKNOWN"
  return <Tag color={mapped?.color ?? "default"}>{label}</Tag>
}
