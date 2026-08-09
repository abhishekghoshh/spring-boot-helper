import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
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
      setError('Invalid username or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="#f5f5f5">
      <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: '100%' }}>
        <Typography variant="h4" textAlign="center" gutterBottom>LoanSphere</Typography>
        <Typography variant="body2" textAlign="center" color="text.secondary" mb={3}>
          Enterprise Digital Banking Platform
        </Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={handleSubmit}>
          <TextField fullWidth label="Username" value={username} onChange={e => setUsername(e.target.value)} margin="normal" required />
          <TextField fullWidth label="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} margin="normal" required />
          <Button fullWidth variant="contained" type="submit" disabled={loading} sx={{ mt: 2, py: 1.5 }}>
            {loading ? 'Signing in...' : 'Sign In'}
          </Button>
        </form>
        <Box textAlign="center" mt={2}>
          <Link to="/register">Create an account</Link>
        </Box>
      </Paper>
    </Box>
  )
}
