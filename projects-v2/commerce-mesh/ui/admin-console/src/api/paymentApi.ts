import apiClient from './apiClient'
import type {
  Payment,
  PaymentQueryParams,
  PaginatedResponse,
  ApiResponse,
} from '../types'

export async function getPayments(
  params: PaymentQueryParams = {}
): Promise<PaginatedResponse<Payment>> {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<Payment>>>('/payments', {
    params,
  })
  return response.data.data
}

export async function getPayment(id: string): Promise<Payment> {
  const response = await apiClient.get<ApiResponse<Payment>>(`/payments/${id}`)
  return response.data.data
}

export interface RefundData {
  amount: number
  reason: string
}

export async function processRefund(paymentId: string, data: RefundData): Promise<Payment> {
  const response = await apiClient.post<ApiResponse<Payment>>(
    `/payments/${paymentId}/refund`,
    data
  )
  return response.data.data
}
