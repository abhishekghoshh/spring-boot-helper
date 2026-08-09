import apiClient from './apiClient'
import type { DashboardStats, OrderTrend, TopProduct, ApiResponse } from '../types'

export async function getDashboardStats(): Promise<DashboardStats> {
  const response = await apiClient.get<ApiResponse<DashboardStats>>('/dashboard/stats')
  return response.data.data
}

export async function getOrderTrends(): Promise<OrderTrend[]> {
  const response = await apiClient.get<ApiResponse<OrderTrend[]>>('/dashboard/order-trends')
  return response.data.data
}

export async function getTopProducts(): Promise<TopProduct[]> {
  const response = await apiClient.get<ApiResponse<TopProduct[]>>('/dashboard/top-products')
  return response.data.data
}
