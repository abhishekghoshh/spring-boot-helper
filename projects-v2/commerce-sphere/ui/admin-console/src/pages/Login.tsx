import { useState } from 'react'
import { Box, Card, TextField, Button, Typography, Alert } from '@mui/material'
import { useAuth } from '../hooks/useAuth'

export default function Login() {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setLoading(true)
    try { await login(username, password) }
    catch { setError('Invalid username or password') }
    finally { setLoading(false) }
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="#f0f2f5">
      <Card sx={{ p: 4, width: 400 }}>
        <Typography variant="h4" textAlign="center" mb={3}>CommerceSphere</Typography>
        <Typography variant="h6" textAlign="center" mb={3} color="text.secondary">Admin Login</Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={handleSubmit}>
          <TextField fullWidth label="Username" value={username} onChange={e => setUsername(e.target.value)} margin="normal" required />
          <TextField fullWidth label="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} margin="normal" required />
          <Button fullWidth type="submit" variant="contained" size="large" disabled={loading} sx={{ mt: 2 }}>
            {loading ? 'Signing in...' : 'Sign In'}
          </Button>
        </form>
      </Card>
    </Box>
  )
}
