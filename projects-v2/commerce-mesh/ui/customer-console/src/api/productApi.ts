import apiClient from './apiClient';
import type { ApiResponse, Category, PaginatedResponse, Product, ProductFilters } from '../types';

export const productApi = {
  getProducts: async (filters: ProductFilters = {}): Promise<PaginatedResponse<Product>> => {
    const params: Record<string, string | number | boolean> = {};
    if (filters.category) params.category = filters.category;
    if (filters.minPrice !== undefined) params.minPrice = filters.minPrice;
    if (filters.maxPrice !== undefined) params.maxPrice = filters.maxPrice;
    if (filters.minRating !== undefined) params.minRating = filters.minRating;
    if (filters.sort) params.sort = filters.sort;
    if (filters.search) params.search = filters.search;
    if (filters.page) params.page = filters.page;
    if (filters.pageSize) params.pageSize = filters.pageSize;
    if (filters.inStock !== undefined) params.inStock = filters.inStock;

    const res = await apiClient.get<ApiResponse<PaginatedResponse<Product>>>('/products', { params });
    return res.data.data;
  },

  getProduct: async (id: string): Promise<Product> => {
    const res = await apiClient.get<ApiResponse<Product>>(`/products/${id}`);
    return res.data.data;
  },

  getFeaturedProducts: async (): Promise<Product[]> => {
    const res = await apiClient.get<ApiResponse<Product[]>>('/products/featured');
    return res.data.data;
  },

  getCategories: async (): Promise<Category[]> => {
    const res = await apiClient.get<ApiResponse<Category[]>>('/categories');
    return res.data.data;
  },

  getProductReviews: async (productId: string, page = 1, pageSize = 10) => {
    const res = await apiClient.get(`/products/${productId}/reviews`, {
      params: { page, pageSize },
    });
    return res.data.data;
  },

  createReview: async (productId: string, data: { rating: number; title: string; comment: string }) => {
    const res = await apiClient.post(`/products/${productId}/reviews`, data);
    return res.data.data;
  },
};
