import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { productApi } from '../api/productApi'

export function useProducts(params: Record<string, any> = {}) {
  return useQuery({ queryKey: ['products', params], queryFn: () => productApi.search(params) })
}
export function useProduct(id: string) {
  return useQuery({ queryKey: ['products', id], queryFn: () => productApi.getById(id), enabled: !!id })
}
export function useCreateProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: productApi.create, onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }) })
}
export function useUpdateProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: any }) => productApi.update(id, data), onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }) })
}
export function useDeleteProduct() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: productApi.delete, onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }) })
}
