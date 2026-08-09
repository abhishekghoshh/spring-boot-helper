import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Box, Button, TextField, Typography, Paper, Alert } from '@mui/material'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/')
    } catch {
      setError('Invalid credentials or insufficient permissions')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="#0a1929">
      <Paper elevation={6} sx={{ p: 4, maxWidth: 420, width: '100%', borderRadius: 2 }}>
        <Typography variant="h4" textAlign="center" gutterBottom sx={{ color: '#1565c0', fontWeight: 700 }}>
          LoanSphere
        </Typography>
        <Typography variant="body2" textAlign="center" color="text.secondary" mb={3}>
          Admin Console — Authorized Personnel Only
        </Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth label="Username" value={username}
            onChange={e => setUsername(e.target.value)} margin="normal"
            required autoFocus
          />
          <TextField
            fullWidth label="Password" type="password" value={password}
            onChange={e => setPassword(e.target.value)} margin="normal"
            required
          />
          <Button fullWidth variant="contained" type="submit" disabled={loading}
            sx={{ mt: 3, py: 1.5, bgcolor: '#1565c0' }}>
            {loading ? 'Authenticating...' : 'Sign In'}
          </Button>
        </form>
      </Paper>
    </Box>
  )
}
