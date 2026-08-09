import client from './client'

export const productApi = {
  search: (params: Record<string, string | number | boolean>) =>
    client.get('/api/catalog/products', { params }).then(r => r.data),
  getById: (id: string) =>
    client.get(`/api/catalog/products/${id}`).then(r => r.data),
  create: (data: any) =>
    client.post('/api/catalog/products', data).then(r => r.data),
  update: (id: string, data: any) =>
    client.put(`/api/catalog/products/${id}`, data).then(r => r.data),
  delete: (id: string) =>
    client.delete(`/api/catalog/products/${id}`),
  uploadImage: (id: string, file: File) => {
    const fd = new FormData(); fd.append('file', file)
    return client.post(`/api/catalog/products/${id}/images`, fd).then(r => r.data)
  },
  getFeatured: (params: Record<string, string | number>) =>
    client.get('/api/catalog/products/featured', { params }).then(r => r.data)
}
