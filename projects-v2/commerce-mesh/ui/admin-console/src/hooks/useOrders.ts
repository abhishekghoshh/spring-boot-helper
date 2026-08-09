import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as orderApi from '../api/orderApi'
import type { OrderQueryParams, OrderStatus } from '../types'

export function useOrders(params: OrderQueryParams = {}) {
  return useQuery({
    queryKey: ['orders', params],
    queryFn: () => orderApi.getOrders(params),
  })
}

export function useOrder(id: string) {
  return useQuery({
    queryKey: ['orders', id],
    queryFn: () => orderApi.getOrder(id),
    enabled: !!id,
  })
}

export function useUpdateOrderStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      status,
      trackingNumber,
    }: {
      id: string
      status: OrderStatus
      trackingNumber?: string
    }) => orderApi.updateOrderStatus(id, status, trackingNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
}
