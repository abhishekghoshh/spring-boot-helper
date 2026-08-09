import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { Box, Button, TextField, Typography, Paper, Alert } from '@mui/material'

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '', firstName: '', lastName: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)
  const { register } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register(form)
      setSuccess(true)
    } catch {
      setError('Registration failed')
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="#f5f5f5">
        <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: '100%', textAlign: 'center' }}>
          <Alert severity="success" sx={{ mb: 2 }}>Registration successful!</Alert>
          <Link to="/login">Click here to sign in</Link>
        </Paper>
      </Box>
    )
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="#f5f5f5">
      <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: '100%' }}>
        <Typography variant="h4" textAlign="center" gutterBottom>Register</Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={handleSubmit}>
          <TextField fullWidth label="First Name" value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })} margin="normal" required />
          <TextField fullWidth label="Last Name" value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })} margin="normal" required />
          <TextField fullWidth label="Username" value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} margin="normal" required />
          <TextField fullWidth label="Email" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} margin="normal" required />
          <TextField fullWidth label="Password" type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} margin="normal" required />
          <Button fullWidth variant="contained" type="submit" disabled={loading} sx={{ mt: 2, py: 1.5 }}>
            {loading ? 'Registering...' : 'Register'}
          </Button>
        </form>
        <Box textAlign="center" mt={2}><Link to="/login">Already have an account?</Link></Box>
      </Paper>
    </Box>
  )
}
