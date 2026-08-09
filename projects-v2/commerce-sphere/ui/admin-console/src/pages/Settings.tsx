import { Box, Typography, Card, CardContent, TextField, Button, Alert } from '@mui/material'
import { useState } from 'react'
import { authApi } from '../api/authApi'

export default function Settings() {
  const [currentPassword, setCurrent] = useState('')
  const [newPassword, setNew] = useState('')
  const [msg, setMsg] = useState('')

  const handleChange = async () => {
    try { await authApi.changePassword(currentPassword, newPassword); setMsg('Password changed!') }
    catch { setMsg('Failed to change password') }
  }

  return (
    <Box>
      <Typography variant="h4" mb={3}>Settings</Typography>
      {msg && <Alert severity={msg.includes('Failed') ? 'error' : 'success'} sx={{ mb: 2 }}>{msg}</Alert>}
      <Card><CardContent>
        <Typography variant="h6" mb={2}>Change Password</Typography>
        <TextField fullWidth type="password" label="Current Password" value={currentPassword} onChange={e => setCurrent(e.target.value)} margin="normal" />
        <TextField fullWidth type="password" label="New Password" value={newPassword} onChange={e => setNew(e.target.value)} margin="normal" />
        <Button variant="contained" onClick={handleChange} sx={{ mt: 2 }}>Change Password</Button>
      </CardContent></Card>
    </Box>
  )
}
