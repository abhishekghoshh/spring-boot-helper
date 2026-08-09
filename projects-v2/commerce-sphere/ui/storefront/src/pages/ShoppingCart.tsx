import { Container, Typography, List, ListItem, ListItemText, IconButton, Button, Box, TextField } from '@mui/material'; import DeleteIcon from '@mui/icons-material/Delete'; import { useCart } from '../hooks/useCart'; import { useNavigate } from 'react-router-dom'
export default function ShoppingCart() {
  const { cart, removeItem, updateQty } = useCart(); const navigate = useNavigate()
  if (!cart || !cart.items?.length) return <Container sx={{ mt: 4 }}><Typography variant="h4">Cart</Typography><Typography mt={2}>Your cart is empty.</Typography></Container>
  return <Container sx={{ mt: 4 }}><Typography variant="h4" mb={3}>Shopping Cart</Typography>
    <List>{cart.items.map((i: any) => <ListItem key={i.productId} secondaryAction={<IconButton edge="end" onClick={() => removeItem(i.productId)}><DeleteIcon /></IconButton>}>
      <ListItemText primary={i.productName} secondary={`$${i.price} each`} />
      <TextField type="number" size="small" value={i.quantity} onChange={e => updateQty(i.productId, Math.max(1, Number(e.target.value)))} sx={{ width: 70, mr: 2 }} />
    </ListItem>)}</List>
    <Box textAlign="right"><Typography variant="h5">Total: ${cart.totalAmount}</Typography>
      <Button variant="contained" size="large" onClick={() => navigate('/checkout')} sx={{ mt: 2 }}>Proceed to Checkout</Button></Box>
  </Container>
}
