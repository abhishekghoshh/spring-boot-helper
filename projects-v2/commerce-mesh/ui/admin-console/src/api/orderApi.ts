import apiClient from './apiClient'
import type {
  Order,
  OrderStatus,
  OrderQueryParams,
  PaginatedResponse,
  ApiResponse,
} from '../types'

export async function getOrders(
  params: OrderQueryParams = {}
): Promise<PaginatedResponse<Order>> {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<Order>>>('/orders', {
    params,
  })
  return response.data.data
}

export async function getOrder(id: string): Promise<Order> {
  const response = await apiClient.get<ApiResponse<Order>>(`/orders/${id}`)
  return response.data.data
}

export async function updateOrderStatus(
  id: string,
  status: OrderStatus,
  trackingNumber?: string
): Promise<Order> {
  const response = await apiClient.put<ApiResponse<Order>>(`/orders/${id}/status`, {
    status,
    trackingNumber,
  })
  return response.data.data
}
