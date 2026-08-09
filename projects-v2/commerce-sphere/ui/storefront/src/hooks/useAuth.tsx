import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'; import { useNavigate } from 'react-router-dom'; import { authApi } from '../api/authApi'
interface User { username: string; email: string; id?: string }
interface AC { user: User | null; isAuthenticated: boolean; loading: boolean; login: (u: string, p: string) => Promise<void>; registerUser: (d: any) => Promise<void>; logout: () => void }
const C = createContext<AC>({ user: null, isAuthenticated: false, loading: true, login: async () => {}, registerUser: async () => {}, logout: () => {} })
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null); const [loading, setLoading] = useState(true); const navigate = useNavigate()
  useEffect(() => { const s = localStorage.getItem('user'); if (s) setUser(JSON.parse(s)); setLoading(false) }, [])
  const login = useCallback(async (u: string, p: string) => { const r = await authApi.login(u, p); const d = { username: r.username, email: r.email, id: r.userId }; localStorage.setItem('accessToken', r.accessToken); localStorage.setItem('user', JSON.stringify(d)); setUser(d) }, [])
  const registerUser = useCallback(async (d: any) => { const r = await authApi.register(d); const u = { username: r.username, email: r.email }; localStorage.setItem('accessToken', r.accessToken); localStorage.setItem('user', JSON.stringify(u)); setUser(u) }, [])
  const logout = useCallback(() => { localStorage.removeItem('accessToken'); localStorage.removeItem('user'); setUser(null); navigate('/') }, [navigate])
  return <C.Provider value={{ user, isAuthenticated: !!user, loading, login, registerUser, logout }}>{children}</C.Provider>
}
export function useAuth() { return useContext(C) }
