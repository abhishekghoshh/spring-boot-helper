import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as paymentApi from '../api/paymentApi'
import type { PaymentQueryParams } from '../types'

export function usePayments(params: PaymentQueryParams = {}) {
  return useQuery({
    queryKey: ['payments', params],
    queryFn: () => paymentApi.getPayments(params),
  })
}

export function usePayment(id: string) {
  return useQuery({
    queryKey: ['payments', id],
    queryFn: () => paymentApi.getPayment(id),
    enabled: !!id,
  })
}

export function useProcessRefund() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      paymentId,
      data,
    }: {
      paymentId: string
      data: Parameters<typeof paymentApi.processRefund>[1]
    }) => paymentApi.processRefund(paymentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })
}
