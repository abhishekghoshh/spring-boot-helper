import apiClient from './apiClient';
import type { ApiResponse, Cart } from '../types';

export const cartApi = {
  getCart: async (): Promise<Cart> => {
    const res = await apiClient.get<ApiResponse<Cart>>('/cart');
    return res.data.data;
  },

  addToCart: async (productId: string, quantity: number = 1): Promise<Cart> => {
    const res = await apiClient.post<ApiResponse<Cart>>('/cart/items', { productId, quantity });
    return res.data.data;
  },

  updateCartItem: async (productId: string, quantity: number): Promise<Cart> => {
    const res = await apiClient.put<ApiResponse<Cart>>(`/cart/items/${productId}`, { quantity });
    return res.data.data;
  },

  removeFromCart: async (productId: string): Promise<Cart> => {
    const res = await apiClient.delete<ApiResponse<Cart>>(`/cart/items/${productId}`);
    return res.data.data;
  },

  clearCart: async (): Promise<void> => {
    await apiClient.delete('/cart');
  },

  applyCoupon: async (code: string): Promise<Cart> => {
    const res = await apiClient.post<ApiResponse<Cart>>('/cart/coupon', { code });
    return res.data.data;
  },
};
