import apiClient from './apiClient';
import type { ApiResponse, CreateOrderRequest, Order, PaginatedResponse } from '../types';

export const orderApi = {
  createOrder: async (data: CreateOrderRequest): Promise<Order> => {
    const res = await apiClient.post<ApiResponse<Order>>('/orders', data);
    return res.data.data;
  },

  getOrders: async (page = 1, pageSize = 10): Promise<PaginatedResponse<Order>> => {
    const res = await apiClient.get<ApiResponse<PaginatedResponse<Order>>>('/orders', {
      params: { page, pageSize },
    });
    return res.data.data;
  },

  getOrder: async (id: string): Promise<Order> => {
    const res = await apiClient.get<ApiResponse<Order>>(`/orders/${id}`);
    return res.data.data;
  },

  cancelOrder: async (id: string): Promise<Order> => {
    const res = await apiClient.put<ApiResponse<Order>>(`/orders/${id}/cancel`);
    return res.data.data;
  },
};
