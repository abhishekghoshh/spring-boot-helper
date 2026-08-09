import apiClient from './apiClient'
import type { InventoryItem, ApiResponse } from '../types'

export async function getInventory(productId?: string): Promise<InventoryItem[]> {
  const response = await apiClient.get<ApiResponse<InventoryItem[]>>('/inventory', {
    params: productId ? { productId } : undefined,
  })
  return response.data.data
}

export interface InventoryUpdateData {
  quantity: number
  reserved?: number
  reorderLevel?: number
  reorderQuantity?: number
}

export async function updateInventory(
  productId: string,
  warehouseId: string,
  data: InventoryUpdateData
): Promise<InventoryItem> {
  const response = await apiClient.put<ApiResponse<InventoryItem>>(
    `/inventory/${productId}/warehouse/${warehouseId}`,
    data
  )
  return response.data.data
}

export interface StockCheckResult {
  productId: string
  productName: string
  requestedQuantity: number
  available: boolean
  availableQuantity: number
}

export async function checkStock(
  productId: string,
  quantity: number
): Promise<StockCheckResult> {
  const response = await apiClient.get<ApiResponse<StockCheckResult>>(
    `/inventory/${productId}/check`,
    { params: { quantity } }
  )
  return response.data.data
}
