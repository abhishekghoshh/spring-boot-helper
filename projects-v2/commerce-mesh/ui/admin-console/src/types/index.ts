export interface User {
  id: string
  username: string
  email: string
  fullName: string
  roles: string[]
  verified: boolean
  enabled: boolean
  createdAt: string
}

export interface Product {
  id: string
  name: string
  description: string
  brand: string
  price: number
  categoryId: string
  categoryName: string
  tags: string[]
  images: string[]
  rating: number
  reviewCount: number
  status: 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED'
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: string
  name: string
  slug: string
  description: string
  parentId: string | null
  children: Category[]
  productCount: number
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export interface OrderItem {
  productId: string
  productName: string
  productImage: string
  quantity: number
  unitPrice: number
  total: number
}

export interface ShippingAddress {
  fullName: string
  address: string
  city: string
  state: string
  zipCode: string
  country: string
  phone: string
}

export interface OrderTimelineEntry {
  status: string
  timestamp: string
  note: string
}

export interface Order {
  id: string
  orderNumber: string
  customerId: string
  customerName: string
  items: OrderItem[]
  total: number
  status: OrderStatus
  shippingAddress: ShippingAddress
  trackingNumber: string | null
  timeline: OrderTimelineEntry[]
  createdAt: string
  updatedAt: string
}

export type PaymentStatus =
  | 'PENDING'
  | 'COMPLETED'
  | 'FAILED'
  | 'REFUNDED'
  | 'PARTIALLY_REFUNDED'

export interface Payment {
  id: string
  transactionId: string
  orderId: string
  orderNumber: string
  customerName: string
  amount: number
  method: string
  status: PaymentStatus
  createdAt: string
}

export interface InventoryItem {
  productId: string
  productName: string
  productImage: string
  sku: string
  warehouseId: string
  warehouseName: string
  quantity: number
  reserved: number
  available: number
  reorderLevel: number
  reorderQuantity: number
  updatedAt: string
}

export interface Customer {
  id: string
  username: string
  email: string
  fullName: string
  ordersCount: number
  totalSpent: number
  joinedDate: string
  status: 'ACTIVE' | 'INACTIVE'
  phone: string
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface LoginCredentials {
  username: string
  password: string
}

export interface RegisterData {
  username: string
  email: string
  password: string
  fullName: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface ApiResponse<T> {
  data: T
  message: string
  success: boolean
}

export interface ProductQueryParams {
  page?: number
  size?: number
  search?: string
  categoryId?: string
  brand?: string
  status?: string
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export interface OrderQueryParams {
  page?: number
  size?: number
  status?: OrderStatus
  customerId?: string
  startDate?: string
  endDate?: string
}

export interface PaymentQueryParams {
  page?: number
  size?: number
  status?: PaymentStatus
  orderId?: string
  startDate?: string
  endDate?: string
}

export interface UserQueryParams {
  page?: number
  size?: number
  search?: string
  role?: string
}

export interface DashboardStats {
  activeUsers: number
  ordersToday: number
  revenue: number
  productsCount: number
  lowStockCount: number
  failedPayments: number
}

export interface OrderTrend {
  date: string
  orders: number
  revenue: number
}

export interface TopProduct {
  productId: string
  productName: string
  quantity: number
  revenue: number
}
