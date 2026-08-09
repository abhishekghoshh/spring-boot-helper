import axios from 'axios'
const client = axios.create({ baseURL: '/' })
client.interceptors.request.use((c) => { const t = localStorage.getItem('accessToken'); if (t) c.headers.Authorization = `Bearer ${t}`; return c })
client.interceptors.response.use((r) => r, (e) => { if (e.response?.status === 401) { localStorage.removeItem('accessToken'); } return Promise.reject(e) })
export default client
