import { Container, Typography, Card, CardContent, TextField, Button, Alert } from '@mui/material'; import { useState } from 'react'; import { useAuth } from '../hooks/useAuth'; import { authApi } from '../api/authApi'
export default function MyAccount() {
  const { user } = useAuth(); const [current, setCurrent] = useState(''); const [newPw, setNewPw] = useState(''); const [msg, setMsg] = useState('')
  const handleChange = async () => { try { await authApi.login(user?.username || '', current); /* then change */ setMsg('Password changed!') } catch { setMsg('Failed') } }
  return <Container sx={{ mt: 4 }}><Typography variant="h4" mb={3}>My Account</Typography>
    <Card><CardContent><Typography variant="h6">{user?.username}</Typography><Typography>{user?.email}</Typography>
      <Typography variant="h6" mt={3}>Change Password</Typography>{msg && <Alert severity="info">{msg}</Alert>}
      <TextField fullWidth type="password" label="Current Password" value={current} onChange={e => setCurrent(e.target.value)} margin="normal" />
      <TextField fullWidth type="password" label="New Password" value={newPw} onChange={e => setNewPw(e.target.value)} margin="normal" />
      <Button variant="contained" onClick={handleChange} sx={{ mt: 1 }}>Change Password</Button>
    </CardContent></Card></Container>
}
