import client from './client'
export const authApi = { login: (u: string, p: string) => client.post('/api/auth/login', { username: u, password: p }).then(r => r.data), register: (d: any) => client.post('/api/auth/register', d).then(r => r.data), getProfile: () => client.get('/api/users/profile').then(r => r.data) }
