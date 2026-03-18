import type { ReactNode } from "react"

type StatusKind =
  | "NORMAL"
  | "LOW_STOCK"
  | "EXPIRING_SOON"
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "HIGH"
  | "MEDIUM"
  | "LOW"
  | "CRITICAL"

type StatusBadgeProps = {
  kind: string
  children?: ReactNode
}

const styles: Record<StatusKind, string> = {
  NORMAL: "bg-emerald-100 text-emerald-800 border-emerald-200",
  LOW_STOCK: "bg-rose-100 text-rose-800 border-rose-200",
  EXPIRING_SOON: "bg-amber-100 text-amber-800 border-amber-200",
  OPEN: "bg-slate-100 text-slate-700 border-slate-200",
  IN_PROGRESS: "bg-sky-100 text-sky-800 border-sky-200",
  RESOLVED: "bg-emerald-100 text-emerald-800 border-emerald-200",
  HIGH: "bg-rose-100 text-rose-800 border-rose-200",
  MEDIUM: "bg-orange-100 text-orange-800 border-orange-200",
  LOW: "bg-yellow-100 text-yellow-800 border-yellow-200",
  CRITICAL: "bg-red-100 text-red-800 border-red-200",
}

export default function StatusBadge({ kind, children }: StatusBadgeProps) {
  const normalized = kind.toUpperCase() as StatusKind
  const className = styles[normalized] ?? "bg-slate-100 text-slate-700 border-slate-200"

  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold tracking-wide ${className}`}
    >
      {children ?? normalized.replaceAll("_", " ")}
    </span>
  )
}
