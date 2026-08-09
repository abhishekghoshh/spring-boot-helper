import client from './client'
export const orderApi = { checkout: (d: any) => client.post('/api/orders/checkout', d).then(r => r.data), listByUser: (uid: string) => client.get(`/api/orders/user/${uid}`).then(r => r.data), getById: (id: string) => client.get(`/api/orders/${id}`).then(r => r.data), cancel: (id: string) => client.post(`/api/orders/${id}/cancel`).then(r => r.data) }
