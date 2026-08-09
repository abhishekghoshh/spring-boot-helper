import client from './client'

export const categoryApi = {
  list: () => client.get('/api/catalog/categories').then(r => r.data),
  getById: (id: string) => client.get(`/api/catalog/categories/${id}`).then(r => r.data),
  create: (data: any) => client.post('/api/catalog/categories', data).then(r => r.data),
  update: (id: string, data: any) => client.put(`/api/catalog/categories/${id}`, data).then(r => r.data),
  delete: (id: string) => client.delete(`/api/catalog/categories/${id}`)
}

export const brandApi = {
  list: () => client.get('/api/catalog/brands').then(r => r.data),
  create: (data: any) => client.post('/api/catalog/brands', data).then(r => r.data),
  update: (id: string, data: any) => client.put(`/api/catalog/brands/${id}`, data).then(r => r.data),
  delete: (id: string) => client.delete(`/api/catalog/brands/${id}`)
}
