import apiClient from './apiClient'
import type { User, UserQueryParams, PaginatedResponse, ApiResponse } from '../types'

export async function getUsers(params: UserQueryParams = {}): Promise<PaginatedResponse<User>> {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<User>>>('/users', {
    params,
  })
  return response.data.data
}

export async function getUser(id: string): Promise<User> {
  const response = await apiClient.get<ApiResponse<User>>(`/users/${id}`)
  return response.data.data
}

export async function updateUser(
  id: string,
  data: Partial<Pick<User, 'email' | 'fullName' | 'enabled'>>
): Promise<User> {
  const response = await apiClient.put<ApiResponse<User>>(`/users/${id}`, data)
  return response.data.data
}

export async function assignRole(userId: string, roleId: string): Promise<User> {
  const response = await apiClient.post<ApiResponse<User>>(`/users/${userId}/roles`, {
    roleId,
  })
  return response.data.data
}

export async function removeRole(userId: string, roleId: string): Promise<User> {
  const response = await apiClient.delete<ApiResponse<User>>(
    `/users/${userId}/roles/${roleId}`
  )
  return response.data.data
}
