import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/authApi'

interface User { username: string; email: string; fullName?: string; roles?: string[] }
interface AuthContextType {
  user: User | null; isAuthenticated: boolean; loading: boolean
  login: (username: string, password: string) => Promise<void>; logout: () => void
}

const AuthContext = createContext<AuthContextType>({ user: null, isAuthenticated: false, loading: true, login: async () => {}, logout: () => {} })

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const stored = localStorage.getItem('user')
    const token = localStorage.getItem('accessToken')
    if (stored && token) setUser(JSON.parse(stored))
    setLoading(false)
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    const userData = { username: res.username, email: res.email, fullName: res.fullName, roles: res.roles || [] }
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    localStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
    navigate('/dashboard')
  }, [navigate])

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setUser(null)
    navigate('/login')
  }, [navigate])

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, loading, login, logout }}>{children}</AuthContext.Provider>
  )
}

export function useAuth() { return useContext(AuthContext) }
