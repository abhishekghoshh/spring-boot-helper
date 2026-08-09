import apiClient from './apiClient';
import type { ApiResponse } from '../types';
import type { Product } from '../types';

export const wishlistApi = {
  getWishlist: async (): Promise<Product[]> => {
    const res = await apiClient.get<ApiResponse<Product[]>>('/wishlist');
    return res.data.data;
  },

  addToWishlist: async (productId: string): Promise<void> => {
    await apiClient.post('/wishlist/items', { productId });
  },

  removeFromWishlist: async (productId: string): Promise<void> => {
    await apiClient.delete(`/wishlist/items/${productId}`);
  },

  isInWishlist: async (productId: string): Promise<boolean> => {
    const res = await apiClient.get<ApiResponse<boolean>>(`/wishlist/items/${productId}/exists`);
    return res.data.data;
  },
};
