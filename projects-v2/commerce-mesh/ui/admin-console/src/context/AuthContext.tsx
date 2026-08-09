import {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  useMemo,
  ReactNode,
} from 'react'
import type { User, LoginCredentials, RegisterData } from '../types'
import * as authApi from '../api/authApi'

interface AuthContextType {
  isAuthenticated: boolean
  isLoading: boolean
  user: User | null
  roles: string[]
  login: (credentials: LoginCredentials) => Promise<void>
  register: (data: RegisterData) => Promise<void>
  logout: () => void
  refreshAuth: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const restoreSession = useCallback(async () => {
    const accessToken = localStorage.getItem('accessToken')
    const refreshToken = localStorage.getItem('refreshToken')
    const storedUser = localStorage.getItem('user')

    if (!accessToken || !refreshToken) {
      setIsLoading(false)
      return
    }

    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch {
        localStorage.removeItem('user')
      }
    }

    try {
      const authResponse = await authApi.refreshToken(refreshToken)
      localStorage.setItem('accessToken', authResponse.accessToken)
      localStorage.setItem('refreshToken', authResponse.refreshToken)
      if (authResponse.user) {
        setUser(authResponse.user)
        localStorage.setItem('user', JSON.stringify(authResponse.user))
      }
    } catch {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    restoreSession()
  }, [restoreSession])

  const login = useCallback(async (credentials: LoginCredentials) => {
    const authResponse = await authApi.login(credentials)
    localStorage.setItem('accessToken', authResponse.accessToken)
    localStorage.setItem('refreshToken', authResponse.refreshToken)
    if (authResponse.user) {
      localStorage.setItem('user', JSON.stringify(authResponse.user))
      setUser(authResponse.user)
    } else {
      setUser({
        id: '',
        username: credentials.username,
        email: '',
        fullName: '',
        roles: ['ADMIN'],
        verified: true,
        enabled: true,
        createdAt: '',
      })
    }
  }, [])

  const register = useCallback(async (data: RegisterData) => {
    const authResponse = await authApi.register(data)
    localStorage.setItem('accessToken', authResponse.accessToken)
    localStorage.setItem('refreshToken', authResponse.refreshToken)
    if (authResponse.user) {
      localStorage.setItem('user', JSON.stringify(authResponse.user))
      setUser(authResponse.user)
    }
  }, [])

  const logout = useCallback(() => {
    authApi.logout().catch(() => {})
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setUser(null)
  }, [])

  const refreshAuth = useCallback(async () => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) return
    const authResponse = await authApi.refreshToken(refreshToken)
    localStorage.setItem('accessToken', authResponse.accessToken)
    localStorage.setItem('refreshToken', authResponse.refreshToken)
    if (authResponse.user) {
      setUser(authResponse.user)
    }
  }, [])

  const roles = useMemo(() => user?.roles ?? [], [user])

  const value = useMemo(
    () => ({
      isAuthenticated: !!user,
      isLoading,
      user,
      roles,
      login,
      register,
      logout,
      refreshAuth,
    }),
    [user, isLoading, roles, login, register, logout, refreshAuth]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (ctx === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
