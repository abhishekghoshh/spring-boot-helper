import { Container, Typography, TextField, Button, Box } from '@mui/material'; import { useState } from 'react'; import { useNavigate } from 'react-router-dom'; import { useAuth } from '../hooks/useAuth'; import { useCart } from '../hooks/useCart'; import { orderApi } from '../api/orderApi'
export default function Checkout() {
  const { user } = useAuth(); const { cart, cartId } = useCart(); const navigate = useNavigate()
  const [address, setAddress] = useState(''); const [loading, setLoading] = useState(false)

  if (!cart?.items?.length) return <Container sx={{ mt: 4 }}><Typography>Your cart is empty.</Typography></Container>
  if (!user) return <Container sx={{ mt: 4 }}><Typography>Please <Button onClick={() => navigate('/login')}>login</Button> to checkout.</Typography></Container>

  const handleCheckout = async () => {
    setLoading(true)
    try { const order = await orderApi.checkout({ cartId, userId: user.id, userEmail: user.email, shippingAddress: address }); navigate(`/order-confirmation/${order.id}`) }
    catch { alert('Checkout failed') }
    finally { setLoading(false) }
  }

  return <Container sx={{ mt: 4 }}><Typography variant="h4" mb={3}>Checkout</Typography>
    <Box mb={3}><Typography variant="h6">Order Summary</Typography>{cart.items.map((i: any) => <Typography key={i.productId}>{i.productName} x{i.quantity} - ${i.price * i.quantity}</Typography>)}<Typography variant="h6" mt={1}>Total: ${cart.totalAmount}</Typography></Box>
    <TextField fullWidth label="Shipping Address" multiline rows={2} value={address} onChange={e => setAddress(e.target.value)} required /><Button variant="contained" size="large" onClick={handleCheckout} disabled={loading || !address} sx={{ mt: 2 }}>{loading ? 'Processing...' : 'Place Order'}</Button>
  </Container>
}
