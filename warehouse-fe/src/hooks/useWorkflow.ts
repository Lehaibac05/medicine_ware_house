import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { workflowApi } from "../services/workflow"

export const workflowQueryKeys = {
  inventory: ["workflow", "inventory"] as const,
  requests: ["workflow", "requests"] as const,
  purchaseOrders: ["workflow", "purchaseOrders"] as const,
  purchaseOrderDetail: (id: number) => ["workflow", "purchaseOrder", id] as const,
  goodsReceipts: ["workflow", "goodsReceipts"] as const,
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

export const useRequestsQuery = () => {
  return useQuery({
    queryKey: workflowQueryKeys.requests,
    queryFn: workflowApi.getRequests,
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

export const useGoodsReceiptsQuery = () => {
  return useQuery({
    queryKey: workflowQueryKeys.goodsReceipts,
    queryFn: workflowApi.getGoodsReceipts,
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
    mutationFn: ({ id, notes }: { id: number; notes?: string }) =>
      workflowApi.approveGoodsReceipt(id, true, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.goodsReceipts })
      queryClient.invalidateQueries({ queryKey: workflowQueryKeys.inventory })
    },
  })
}
