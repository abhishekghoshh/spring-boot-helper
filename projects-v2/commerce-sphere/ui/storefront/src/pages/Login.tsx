import { useState } from 'react'; import { Container, Card, TextField, Button, Typography, Alert, Link } from '@mui/material'; import { useNavigate, Link as RouterLink } from 'react-router-dom'; import { useAuth } from '../hooks/useAuth'
export default function Login() {
  const { login } = useAuth(); const navigate = useNavigate(); const [username, setUsername] = useState(''); const [password, setPassword] = useState(''); const [error, setError] = useState('')
  const handleSubmit = async (e: React.FormEvent) => { e.preventDefault(); try { await login(username, password); navigate('/') } catch { setError('Invalid credentials') } }
  return <Container maxWidth="sm" sx={{ mt: 8 }}><Card sx={{ p: 4 }}><Typography variant="h4" mb={3} textAlign="center">Login</Typography>{error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
    <form onSubmit={handleSubmit}><TextField fullWidth label="Username" value={username} onChange={e => setUsername(e.target.value)} margin="normal" /><TextField fullWidth label="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} margin="normal" />
      <Button fullWidth type="submit" variant="contained" size="large" sx={{ mt: 2 }}>Sign In</Button></form>
    <Typography textAlign="center" mt={2}>Don't have an account? <Link component={RouterLink} to="/register">Register</Link></Typography></Card></Container>
}
