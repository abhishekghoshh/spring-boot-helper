import { useParams } from 'react-router-dom'; import { Box, Container, Grid, Typography, Button, Chip, TextField, Rating } from '@mui/material'; import { useQuery } from '@tanstack/react-query'; import { productApi, reviewApi } from '../api/productApi'; import { useCart } from '../hooks/useCart'; import Loading from '../components/shared/Loading'; import { useState } from 'react'
export default function ProductDetail() {
  const { id } = useParams(); const { addItem } = useCart(); const [qty, setQty] = useState(1)
  const { data: p, isLoading } = useQuery({ queryKey: ['product', id], queryFn: () => productApi.getById(id!) })
  const { data: reviews } = useQuery({ queryKey: ['reviews', id], queryFn: () => reviewApi.getByProduct(id!) })
  if (isLoading) return <Loading />
  return <Container sx={{ mt: 4 }}><Grid container spacing={4}>
    <Grid item xs={12} md={6}><Box component="img" src={p.imageUrls?.[0] || 'https://via.placeholder.com/500x500'} sx={{ width: '100%', borderRadius: 2 }} /></Grid>
    <Grid item xs={12} md={6}>
      <Typography variant="h4">{p.name}</Typography><Chip label={p.categoryName} size="small" sx={{ mr: 1 }} /><Chip label={p.brandName} size="small" color="primary" />
      <Typography variant="h4" color="primary" mt={2}>${p.price}</Typography>
      <Typography variant="body1" mt={2} color="text.secondary">{p.description}</Typography>
      <Box mt={3} display="flex" gap={2}><TextField type="number" size="small" value={qty} onChange={e => setQty(Math.max(1, Number(e.target.value)))} sx={{ width: 80 }} /><Button variant="contained" size="large" onClick={() => addItem({ productId: p.id, productName: p.name, price: p.price, quantity: qty, imageUrl: p.imageUrls?.[0] || '' })}>Add to Cart</Button></Box>
    </Grid>
  </Grid>
  {reviews && <Box mt={4}><Typography variant="h5" mb={2}>Reviews</Typography>{reviews.content?.map((r: any) => <Box key={r.id} mb={2}><Rating value={r.rating} readOnly /><Typography><strong>{r.userName}</strong>: {r.comment}</Typography></Box>)}</Box>}</Container>
}
