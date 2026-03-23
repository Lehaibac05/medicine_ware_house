import { CheckCircleFilled, CloseCircleFilled, ExclamationCircleFilled, InfoCircleFilled } from "@ant-design/icons"
import type { MessageInstance } from "antd/es/message/interface"
import { createElement } from "react"
import { ApiError } from "../services/api"

const STATUS_LABEL: Record<number, string> = {
  400: "Bad request",
  401: "Unauthorized",
  403: "Forbidden",
  404: "Not found",
  409: "Conflict",
  422: "Validation failed",
  500: "Server error",
  502: "Bad gateway",
  503: "Service unavailable",
}

const extractErrorText = (error: unknown, fallback: string) => {
  if (error instanceof ApiError) {
    const statusLabel = STATUS_LABEL[error.status] || "Request failed"
    return `${fallback} (${error.status} ${statusLabel})`
  }

  if (error instanceof Error && error.message.trim()) {
    return `${fallback}: ${error.message}`
  }

  if (typeof error === "object" && error && "response" in error) {
    const response = (error as { response?: { status?: number; data?: unknown } }).response
    const status = response?.status
    const detail = response?.data
    if (status) {
      const statusLabel = STATUS_LABEL[status] || "Request failed"
      if (typeof detail === "string" && detail.trim()) {
        return `${fallback} (${status} ${statusLabel}): ${detail}`
      }
      return `${fallback} (${status} ${statusLabel})`
    }
  }

  return fallback
}

const baseDuration = {
  success: 2.6,
  info: 3,
  warning: 3.5,
  error: 4.5,
}

export const createToast = (messageApi: MessageInstance) => ({
  success: (content: string) => {
    messageApi.open({
      type: "success",
      icon: createElement(CheckCircleFilled),
      content,
      duration: baseDuration.success,
      className: "app-toast app-toast-success",
    })
  },

  info: (content: string) => {
    messageApi.open({
      type: "info",
      icon: createElement(InfoCircleFilled),
      content,
      duration: baseDuration.info,
      className: "app-toast app-toast-info",
    })
  },

  warning: (content: string) => {
    messageApi.open({
      type: "warning",
      icon: createElement(ExclamationCircleFilled),
      content,
      duration: baseDuration.warning,
      className: "app-toast app-toast-warning",
    })
  },

  error: (errorOrContent: unknown, fallback = "Something went wrong") => {
    const content = typeof errorOrContent === "string"
      ? errorOrContent
      : extractErrorText(errorOrContent, fallback)

    messageApi.open({
      type: "error",
      icon: createElement(CloseCircleFilled),
      content,
      duration: baseDuration.error,
      className: "app-toast app-toast-error",
    })
  },
})

export type ToastApi = ReturnType<typeof createToast>
