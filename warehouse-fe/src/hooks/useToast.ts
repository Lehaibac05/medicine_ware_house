import { message } from "antd"
import { useMemo } from "react"
import { createToast } from "../utils/toast"

export const useToast = () => {
  const [messageApi, contextHolder] = message.useMessage()
  const toast = useMemo(() => createToast(messageApi), [messageApi])

  return {
    toast,
    contextHolder,
  }
}
