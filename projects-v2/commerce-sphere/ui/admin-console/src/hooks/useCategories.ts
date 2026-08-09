import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryApi, brandApi } from '../api/categoryApi'

export function useCategories() { return useQuery({ queryKey: ['categories'], queryFn: categoryApi.list }) }
export function useBrands() { return useQuery({ queryKey: ['brands'], queryFn: brandApi.list }) }
export function useCreateCategory() { const qc=useQueryClient(); return useMutation({ mutationFn: categoryApi.create, onSuccess: () => qc.invalidateQueries({queryKey:['categories']}) }) }
export function useDeleteCategory() { const qc=useQueryClient(); return useMutation({ mutationFn: categoryApi.delete, onSuccess: () => qc.invalidateQueries({queryKey:['categories']}) }) }
export function useCreateBrand() { const qc=useQueryClient(); return useMutation({ mutationFn: brandApi.create, onSuccess: () => qc.invalidateQueries({queryKey:['brands']}) }) }
export function useDeleteBrand() { const qc=useQueryClient(); return useMutation({ mutationFn: brandApi.delete, onSuccess: () => qc.invalidateQueries({queryKey:['brands']}) }) }
