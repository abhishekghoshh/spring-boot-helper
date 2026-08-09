import apiClient from './apiClient'
import type {
  Product,
  Category,
  ProductQueryParams,
  PaginatedResponse,
  ApiResponse,
} from '../types'

export async function getProducts(
  params: ProductQueryParams = {}
): Promise<PaginatedResponse<Product>> {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<Product>>>('/products', {
    params,
  })
  return response.data.data
}

export async function getProduct(id: string): Promise<Product> {
  const response = await apiClient.get<ApiResponse<Product>>(`/products/${id}`)
  return response.data.data
}

export async function createProduct(
  data: Omit<Product, 'id' | 'createdAt' | 'updatedAt' | 'categoryName'>
): Promise<Product> {
  const response = await apiClient.post<ApiResponse<Product>>('/products', data)
  return response.data.data
}

export async function updateProduct(id: string, data: Partial<Product>): Promise<Product> {
  const response = await apiClient.put<ApiResponse<Product>>(`/products/${id}`, data)
  return response.data.data
}

export async function deleteProduct(id: string): Promise<void> {
  await apiClient.delete(`/products/${id}`)
}

export async function getCategories(): Promise<Category[]> {
  const response = await apiClient.get<ApiResponse<Category[]>>('/categories')
  return response.data.data
}

export async function createCategory(
  data: Omit<Category, 'id' | 'children' | 'productCount'>
): Promise<Category> {
  const response = await apiClient.post<ApiResponse<Category>>('/categories', data)
  return response.data.data
}

export async function updateCategory(
  id: string,
  data: Partial<Category>
): Promise<Category> {
  const response = await apiClient.put<ApiResponse<Category>>(`/categories/${id}`, data)
  return response.data.data
}

export async function deleteCategory(id: string): Promise<void> {
  await apiClient.delete(`/categories/${id}`)
}
