import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { authApi } from '../api'

interface User {
  id: string; username: string; email: string; firstName: string; lastName: string; roles: string[];
}

interface AuthContextType {
  user: User | null
  token: string | null
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  loading: boolean
}

const ADMIN_ROLES = ['SUPER_ADMIN', 'BANK_ADMIN']

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('adminAccessToken'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (token) {
      authApi.me().then(r => {
        const u = r.data.data
        if (ADMIN_ROLES.some(r => u.roles.includes(r))) {
          setUser(u)
        } else {
          localStorage.clear()
          setToken(null)
        }
      }).catch(() => {
        localStorage.clear()
        setToken(null)
      }).finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [token])

  const login = async (username: string, password: string) => {
    const { data } = await authApi.login({ username, password })
    localStorage.setItem('adminAccessToken', data.data.accessToken)
    localStorage.setItem('adminRefreshToken', data.data.refreshToken)
    setToken(data.data.accessToken)
  }

  const logout = () => {
    authApi.logout().catch(() => {})
    localStorage.clear()
    setUser(null)
    setToken(null)
  }

  const isAdmin = user ? ADMIN_ROLES.some(r => user.roles.includes(r)) : false

  return (
    <AuthContext.Provider value={{ user, token, isAdmin, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
