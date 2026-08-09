import client from './client'

export const authApi = {
  login: (username: string, password: string) =>
    client.post('/api/auth/login', { username, password }).then(r => r.data),
  register: (data: any) =>
    client.post('/api/auth/register', data).then(r => r.data),
  getProfile: () =>
    client.get('/api/users/profile').then(r => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    client.put('/api/users/password', { currentPassword, newPassword }),
  listUsers: () =>
    client.get('/api/admin/users').then(r => r.data)
}
