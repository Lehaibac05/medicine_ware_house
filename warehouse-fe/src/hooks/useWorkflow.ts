import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { workflowApi } from "../services/workflow"

export const workflowQueryKeys = {
  inventory: ["workflow", "inventory"] as const,
  requests: ["workflow", "requests"] as const,
  purchaseOrders: ["workflow", "purchaseOrders"] as const,
  purchaseOrderDetail: (id: number) => ["workflow", "purchaseOrder", id] as const,
  goodsReceipts: ["workflow", "goodsReceipts"] as const,
  goodsReceiptDetail: (id: number) => ["workflow", "goodsReceipt", id] as const,
  supplierInvoices: ["workflow", "supplierInvoices"] as const,
  supplierInvoiceDetail: (id: number) => ["workflow", "supplierInvoice", id] as const,
}

export const useInventoryQuery = (params?: {
  medicineName?: string
  warehouseId?: number
  status?: string
}) => {
  return useQuery({
    queryKey: [...workflowQueryKeys.inventory, params],
    queryFn: () => workflowApi.getInventory(params),
  })
}

export const useRequestsQuery = (params?: {
  page?: number
  size?: number
  status?: string
  medicineName?: string
  startDate?: string
  endDate?: string
}) => {
  return useQuery({
    queryKey: [...workflowQueryKeys.requests, params],
    queryFn: () => workflowApi.getRequests(params),
  })
}

export const useApproveRequestMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => workflowApi.approveRequest(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.requests })
    },
  })
}

export const useRejectRequestMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => workflowApi.rejectRequest(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.requests })
    },
  })
}

export const usePurchaseOrderQuery = (id: number) => {
  return useQuery({
    queryKey: workflowQueryKeys.purchaseOrderDetail(id),
    queryFn: () => workflowApi.getPurchaseOrderById(id),
    enabled: Number.isFinite(id) && id > 0,
  })
}

export const useGoodsReceiptsQuery = (params?: { page?: number; size?: number }) => {
  return useQuery({
    queryKey: [...workflowQueryKeys.goodsReceipts, params],
    queryFn: () => workflowApi.getGoodsReceipts(params),
  })
}

export const useGoodsReceiptDetailQuery = (id?: number) => {
  return useQuery({
    queryKey: id ? workflowQueryKeys.goodsReceiptDetail(id) : ["workflow", "goodsReceipt", "none"],
    queryFn: () => workflowApi.getGoodsReceiptById(id as number),
    enabled: Number.isFinite(id) && (id as number) > 0,
  })
}

export const useCreateGoodsReceiptMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: workflowApi.createGoodsReceipt,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.goodsReceipts })
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.purchaseOrders })
    },
  })
}

export const useApproveGoodsReceiptMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      approved = true,
      notes,
    }: {
      id: number
      approved?: boolean
      notes?: string
    }) => workflowApi.approveGoodsReceipt(id, approved, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.goodsReceipts })
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.inventory })
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.purchaseOrders })
    },
  })
}

export const useSupplierInvoicesQuery = () => {
  return useQuery({
    queryKey: workflowQueryKeys.supplierInvoices,
    queryFn: workflowApi.getSupplierInvoices,
    // Luôn lấy dữ liệu mới nhất cho màn thanh toán/hiển thị QR.
    // Tránh trường hợp cache khiến supplier QR không đổi dù đã cập nhật.
    staleTime: 0,
    refetchOnMount: "always",
  })
}

export const useSupplierInvoiceDetailQuery = (id: number) => {
  return useQuery({
    queryKey: workflowQueryKeys.supplierInvoiceDetail(id),
    queryFn: () => workflowApi.getSupplierInvoiceById(id),
    enabled: Number.isFinite(id) && id > 0,
    staleTime: 0,
    refetchOnMount: "always",
  })
}

export const useCreateSupplierInvoiceMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: workflowApi.createSupplierInvoice,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.supplierInvoices })
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.goodsReceipts })
    },
  })
}

export const useVerifySupplierInvoiceMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, verificationNotes }: { id: number; verificationNotes?: string }) =>
      workflowApi.verifySupplierInvoice(id, { verificationNotes }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.supplierInvoices })
    },
  })
}

export const useRejectSupplierInvoiceMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      workflowApi.rejectSupplierInvoice(id, { reason }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.supplierInvoices })
    },
  })
}

export const usePaySupplierInvoiceMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      amount,
      method,
      transactionReference,
      notes,
    }: {
      id: number
      amount: number
      method?: string
      transactionReference?: string
      notes?: string
    }) => workflowApi.paySupplierInvoice(id, { amount, method, transactionReference, notes }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.supplierInvoices })
    },
  })
}
