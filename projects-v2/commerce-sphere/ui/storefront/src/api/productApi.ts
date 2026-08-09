import client from './client'
export const productApi = { search: (p: any) => client.get('/api/catalog/products', { params: p }).then(r => r.data), getById: (id: string) => client.get(`/api/catalog/products/${id}`).then(r => r.data), getFeatured: (p: any) => client.get('/api/catalog/products/featured', { params: p }).then(r => r.data) }
export const categoryApi = { list: () => client.get('/api/catalog/categories').then(r => r.data) }
export const reviewApi = { getByProduct: (id: string) => client.get(`/api/catalog/reviews/product/${id}`).then(r => r.data), create: (d: any) => client.post('/api/catalog/reviews', d).then(r => r.data) }
