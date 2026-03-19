import { useQuery } from "@tanstack/react-query"
import {
  reportApi,
  type FinancialReportParams,
  type InventoryReportParams,
} from "../services/reports"

export const reportQueryKeys = {
  dashboard: ["reports", "dashboard"] as const,
  inventory: (params: InventoryReportParams) => ["reports", "inventory", params] as const,
  financial: (params: FinancialReportParams) => ["reports", "financial", params] as const,
}

export const useDashboardSummaryQuery = () =>
  useQuery({
    queryKey: reportQueryKeys.dashboard,
    queryFn: reportApi.getDashboardSummary,
  })

export const useInventoryReportQuery = (params: InventoryReportParams) =>
  useQuery({
    queryKey: reportQueryKeys.inventory(params),
    queryFn: () => reportApi.getInventoryReport(params),
  })

export const useFinancialReportQuery = (params: FinancialReportParams) =>
  useQuery({
    queryKey: reportQueryKeys.financial(params),
    queryFn: () => reportApi.getFinancialReport(params),
  })
