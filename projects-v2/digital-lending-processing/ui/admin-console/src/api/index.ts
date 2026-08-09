import api from './client'

export const authApi = {
  login: (data: { username: string; password: string }) =>
    api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout'),
  listUsers: () => api.get('/auth/users'),
  createUser: (data: any) => api.post('/auth/users', data),
  deleteUser: (id: string) => api.delete(`/auth/users/${id}`),
}

export const offerApi = {
  getOffers: () => api.get('/offers'),
  createOffer: (data: any) => api.post('/offers', data),
  updateOffer: (id: string, data: any) => api.put(`/offers/${id}`, data),
  deleteOffer: (id: string) => api.delete(`/offers/${id}`),
}

export const customerApi = {
  getAllCustomers: () => api.get('/customers'),
  getProfile: (id: string) => api.get(`/customers/${id}`),
}

export const applicationApi = {
  getAll: () => api.get('/applications'),
  getApplication: (id: string) => api.get(`/applications/${id}`),
}

export const processingApi = {
  getReviews: () => api.get('/processing/reviews'),
  approve: (id: string, notes: string) =>
    api.post(`/processing/reviews/${id}/approve`, { notes }),
  reject: (id: string, reason: string) =>
    api.post(`/processing/reviews/${id}/reject`, { reason }),
  requestInfo: (id: string, message: string) =>
    api.post(`/processing/reviews/${id}/request-info`, { message }),
}

export const dashboardApi = {
  getStats: () => api.get('/admin/dashboard'),
}
