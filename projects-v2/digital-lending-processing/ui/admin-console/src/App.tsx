import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { CssBaseline, CircularProgress, Box } from '@mui/material'
import AdminLayout from './pages/AdminLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import UsersPage from './pages/UsersPage'
import OffersPage from './pages/OffersPage'
import ApplicationsPage from './pages/ApplicationsPage'
import ApprovalsPage from './pages/ApprovalsPage'
import SettingsPage from './pages/SettingsPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token, loading, isAdmin } = useAuth()
  if (loading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress /></Box>
  if (!isAdmin) return <Navigate to="/login" />
  return <>{children}</>
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { token, loading, isAdmin } = useAuth()
  if (loading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress /></Box>
  if (token && isAdmin) return <Navigate to="/" />
  return <>{children}</>
}

export default function App() {
  return (
    <AuthProvider>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
          <Route element={<ProtectedRoute><AdminLayout /></ProtectedRoute>}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/offers" element={<OffersPage />} />
            <Route path="/applications" element={<ApplicationsPage />} />
            <Route path="/approvals" element={<ApprovalsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
