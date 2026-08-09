import { useParams, useNavigate } from 'react-router-dom'; import { Container, Typography, Button, Box } from '@mui/material'; import CheckCircleIcon from '@mui/icons-material/CheckCircle'
export default function OrderConfirmation() { const { id } = useParams(); const navigate = useNavigate()
  return <Container sx={{ mt: 8, textAlign: 'center' }}><CheckCircleIcon sx={{ fontSize: 80, color: 'success.main' }} /><Typography variant="h3" mt={2}>Order Confirmed!</Typography><Typography variant="h6" mt={1} color="text.secondary">Order ID: {id}</Typography>
    <Box mt={4}><Button variant="contained" onClick={() => navigate('/orders')}>View Orders</Button><Button variant="outlined" onClick={() => navigate('/')} sx={{ ml: 2 }}>Continue Shopping</Button></Box></Container>
}
