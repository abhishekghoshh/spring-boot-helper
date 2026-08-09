// ── Product ──
export interface Product {
  id: string;
  name: string;
  slug: string;
  description: string;
  price: number;
  compareAtPrice?: number;
  images: string[];
  category: string;
  categoryId: string;
  brand: string;
  rating: number;
  reviewCount: number;
  inStock: boolean;
  stockQuantity: number;
  attributes: Record<string, string>;
  createdAt: string;
}

// ── Category ──
export interface Category {
  id: string;
  name: string;
  slug: string;
  image: string;
  description: string;
  productCount: number;
}

// ── Cart ──
export interface CartItem {
  productId: string;
  name: string;
  image: string;
  price: number;
  quantity: number;
}

export interface Cart {
  items: CartItem[];
  subtotal: number;
  discount: number;
  total: number;
  couponCode?: string;
  couponDiscount: number;
}

// ── Order ──
export interface OrderItem {
  id: string;
  productId: string;
  name: string;
  image: string;
  price: number;
  quantity: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  total: number;
  shippingAddress: Address;
  paymentMethod: string;
  trackingInfo?: TrackingInfo;
  createdAt: string;
}

export type OrderStatus = 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface TrackingInfo {
  carrier: string;
  trackingNumber: string;
  estimatedDelivery: string;
}

// ── Address ──
export interface Address {
  id?: string;
  fullName: string;
  street: string;
  city: string;
  state: string;
  postalCode: string;
  phone: string;
  isDefault: boolean;
}

// ── User ──
export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
}

// ── Auth ──
export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// ── Review ──
export interface Review {
  id: string;
  productId: string;
  userId: string;
  username: string;
  rating: number;
  title: string;
  comment: string;
  createdAt: string;
}

export interface CreateReviewRequest {
  rating: number;
  title: string;
  comment: string;
}

// ── Search ──
export interface SearchResult {
  products: Product[];
  totalCount: number;
  suggestions: string[];
}

// ── API ──
export interface ApiResponse<T> {
  data: T;
  message: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface ProductFilters {
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  sort?: 'price-asc' | 'price-desc' | 'rating' | 'newest';
  search?: string;
  page?: number;
  pageSize?: number;
  inStock?: boolean;
}

export interface CreateOrderRequest {
  shippingAddress: Address;
  paymentMethod: string;
}

export interface AddToCartRequest {
  productId: string;
  quantity: number;
}
