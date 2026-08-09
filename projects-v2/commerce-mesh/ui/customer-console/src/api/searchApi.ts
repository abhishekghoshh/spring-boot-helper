import apiClient from './apiClient';
import type { ApiResponse, PaginatedResponse, Product } from '../types';

export const searchApi = {
  search: async (
    keyword: string,
    filters?: Record<string, string>,
  ): Promise<PaginatedResponse<Product>> => {
    const params: Record<string, string> = { q: keyword };
    if (filters) {
      Object.entries(filters).forEach(([k, v]) => {
        params[k] = v;
      });
    }
    const res = await apiClient.get<ApiResponse<PaginatedResponse<Product>>>('/search', { params });
    return res.data.data;
  },

  autocomplete: async (keyword: string): Promise<string[]> => {
    const res = await apiClient.get<ApiResponse<string[]>>('/search/autocomplete', {
      params: { q: keyword },
    });
    return res.data.data;
  },
};
