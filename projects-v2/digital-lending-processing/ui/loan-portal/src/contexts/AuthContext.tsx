import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { authApi } from '../api'

interface User {
  id: string; username: string; email: string; firstName: string; lastName: string; roles: string[];
}

interface AuthContextType {
  user: User | null
  token: string | null
  login: (username: string, password: string) => Promise<void>
  register: (data: { username: string; email: string; password: string; firstName: string; lastName: string }) => Promise<void>
  logout: () => void
  loading: boolean
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('accessToken'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (token) {
      authApi.me().then(r => setUser(r.data.data)).catch(() => setToken(null)).finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [token])

  const login = async (username: string, password: string) => {
    const { data } = await authApi.login({ username, password })
    localStorage.setItem('accessToken', data.data.accessToken)
    localStorage.setItem('refreshToken', data.data.refreshToken)
    setToken(data.data.accessToken)
  }

  const register = async (form: { username: string; email: string; password: string; firstName: string; lastName: string }) => {
    await authApi.register(form)
  }

  const logout = () => {
    authApi.logout().catch(() => {})
    localStorage.clear()
    setUser(null)
    setToken(null)
  }

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
