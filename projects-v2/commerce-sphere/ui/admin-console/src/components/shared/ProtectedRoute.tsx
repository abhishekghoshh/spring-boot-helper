import { Navigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Box, CircularProgress } from '@mui/material'

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth()
  if (loading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress /></Box>
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <>{children}</>
}
