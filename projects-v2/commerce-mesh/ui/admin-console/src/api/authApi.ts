import apiClient from './apiClient'
import type { LoginCredentials, RegisterData, AuthResponse, ApiResponse } from '../types'

export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', credentials)
  return response.data.data
}

export async function register(data: RegisterData): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', data)
  return response.data.data
}

export async function refreshToken(token: string): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', {
    refreshToken: token,
  })
  return response.data.data
}

export async function logout(): Promise<void> {
  try {
    await apiClient.post('/auth/logout')
  } finally {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }
}
