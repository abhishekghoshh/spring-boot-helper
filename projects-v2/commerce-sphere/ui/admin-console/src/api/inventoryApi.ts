import client from './client'

export const inventoryApi = {
  getByProduct: (productId: string) =>
    client.get(`/api/inventory/product/${productId}`).then(r => r.data),
  addStock: (productId: string, warehouseId: string, productName: string, quantity: number) =>
    client.post('/api/inventory/stock', null, { params: { productId, warehouseId, productName, quantity } }).then(r => r.data),
  check: (productId: string, quantity: number) =>
    client.post('/api/inventory/check', { productId, quantity }).then(r => r.data),
  listWarehouses: () =>
    client.get('/api/inventory/warehouses').then(r => r.data)
}

export const orderApi = {
  listByUser: (userId: string, page = 0, size = 20) =>
    client.get(`/api/orders/user/${userId}`, { params: { page, size } }).then(r => r.data),
  getById: (id: string) =>
    client.get(`/api/orders/${id}`).then(r => r.data),
  cancel: (id: string) =>
    client.post(`/api/orders/${id}/cancel`).then(r => r.data)
}
