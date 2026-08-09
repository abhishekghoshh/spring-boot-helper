import { Box, Typography, Button } from '@mui/material'
import { useNavigate } from 'react-router-dom'
export default function NotFound() {
  const navigate = useNavigate()
  return <Box textAlign="center" mt={10}><Typography variant="h2">404</Typography><Typography variant="h5" mb={3}>Page Not Found</Typography><Button variant="contained" onClick={() => navigate('/')}>Go Home</Button></Box>
}
