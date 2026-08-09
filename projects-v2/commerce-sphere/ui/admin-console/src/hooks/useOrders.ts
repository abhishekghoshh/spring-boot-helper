import { useQuery } from '@tanstack/react-query'
import { orderApi } from '../api/inventoryApi'

export function useOrders(userId: string) { return useQuery({ queryKey: ['orders', userId], queryFn: () => orderApi.listByUser(userId), enabled: !!userId }) }
export function useOrder(id: string) { return useQuery({ queryKey: ['orders', id], queryFn: () => orderApi.getById(id), enabled: !!id }) }
