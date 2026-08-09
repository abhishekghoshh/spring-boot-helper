import { Container, Card, TextField, Button, Typography, Link } from '@mui/material'; import { Link as RouterLink } from 'react-router-dom'
export default function ForgotPassword() {
  return <Container maxWidth="sm" sx={{ mt: 8 }}><Card sx={{ p: 4 }}><Typography variant="h4" mb={3} textAlign="center">Forgot Password</Typography>
    <TextField fullWidth label="Email" type="email" margin="normal" />
    <Button fullWidth variant="contained" size="large" sx={{ mt: 2 }}>Send Reset Link</Button>
    <Typography textAlign="center" mt={2}><Link component={RouterLink} to="/login">Back to Login</Link></Typography></Card></Container>
}
