import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as inventoryApi from '../api/inventoryApi'

export function useInventory(productId?: string) {
  return useQuery({
    queryKey: ['inventory', productId ?? 'all'],
    queryFn: () => inventoryApi.getInventory(productId),
  })
}

export function useCheckStock(productId: string, quantity: number) {
  return useQuery({
    queryKey: ['inventory', 'stock-check', productId, quantity],
    queryFn: () => inventoryApi.checkStock(productId, quantity),
    enabled: !!productId && quantity > 0,
  })
}

export function useUpdateInventory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      productId,
      warehouseId,
      data,
    }: {
      productId: string
      warehouseId: string
      data: Parameters<typeof inventoryApi.updateInventory>[2]
    }) => inventoryApi.updateInventory(productId, warehouseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}
