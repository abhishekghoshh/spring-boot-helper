import api from './client'

export const authApi = {
  register: (data: { username: string; email: string; password: string; firstName: string; lastName: string }) =>
    api.post('/auth/register', data),
  login: (data: { username: string; password: string }) =>
    api.post('/auth/login', data),
  refreshToken: (refreshToken: string) =>
    api.post('/auth/refresh-token', { refreshToken }),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
}

export const customerApi = {
  getProfile: () => api.get('/customers/profile'),
  createProfile: (data: any) => api.post('/customers/profile', data),
  updateProfile: (data: any) => api.put('/customers/profile', data),
}

export const offerApi = {
  getOffers: () => api.get('/offers'),
  getOffer: (id: string) => api.get(`/offers/${id}`),
  calculateEmi: (id: string, amount: number, tenure: number) =>
    api.get(`/offers/${id}/emi`, { params: { amount, tenureMonths: tenure } }),
}

export const emiApi = {
  calculate: (params: { principal: number; annualInterestRate: number; tenureMonths: number }) =>
    api.get('/emi/calculate', { params }),
  amortizationSchedule: (params: { principal: number; annualInterestRate: number; tenureMonths: number }) =>
    api.get('/emi/amortization-schedule', { params }),
  prepayment: (params: { principal: number; annualInterestRate: number; tenureMonths: number; prepaymentAmount: number }) =>
    api.get('/emi/prepayment', { params }),
}

export const applicationApi = {
  submit: (data: { offerId: string; offerName: string; loanAmount: number; tenureMonths: number; purpose: string }) =>
    api.post('/applications', data),
  getApplications: () => api.get('/applications'),
  getApplication: (id: string) => api.get(`/applications/${id}`),
}

export const documentApi = {
  upload: (formData: FormData) => api.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getDocuments: () => api.get('/documents'),
  getDocument: (id: string) => api.get(`/documents/${id}`),
  deleteDocument: (id: string) => api.delete(`/documents/${id}`),
}

export const notificationApi = {
  getNotifications: () => api.get('/notifications'),
  markAsRead: (id: string) => api.put(`/notifications/${id}/read`),
}
