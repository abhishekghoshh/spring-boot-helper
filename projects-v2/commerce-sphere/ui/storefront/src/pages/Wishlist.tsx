import { Container, Typography, Grid, Card, CardContent, CardMedia, IconButton } from '@mui/material'; import DeleteIcon from '@mui/icons-material/Delete'; import { useState, useEffect } from 'react'; import { wishlistApi } from '../api/cartApi'; import { useAuth } from '../hooks/useAuth'
export default function Wishlist() {
  const { user } = useAuth(); const [items, setItems] = useState<any[]>([])
  useEffect(() => { if (user?.id) wishlistApi.get(user.id).then(setItems) }, [user])
  return <Container sx={{ mt: 4 }}><Typography variant="h4" mb={3}>Wishlist</Typography>
    {items.length === 0 ? <Typography>Your wishlist is empty.</Typography> : <Grid container spacing={3}>{items.map((i: any) => <Grid item xs={12} sm={6} md={3} key={i.productId}><Card>
      <CardMedia component="img" height="200" image={i.imageUrl || 'https://via.placeholder.com/300x200'} /><CardContent><Typography>{i.productName}</Typography><Typography color="primary">${i.price}</Typography></CardContent></Card></Grid>)}</Grid>}
  </Container>
}
