import { Card, CardMedia, CardContent, Typography, CardActions, Button, IconButton, Rating } from '@mui/material'
import FavoriteIcon from '@mui/icons-material/Favorite'; import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../../hooks/useCart'

export default function ProductCard({ product }: { product: any }) {
  const navigate = useNavigate(); const { addItem } = useCart()
  return <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', cursor: 'pointer' }} onClick={() => navigate(`/products/${product.id}`)}>
    <CardMedia component="img" height="200" image={product.imageUrls?.[0] || 'https://via.placeholder.com/300x200?text=Product'} alt={product.name} />
    <CardContent sx={{ flexGrow: 1 }}><Typography variant="subtitle1" fontWeight={600} noWrap>{product.name}</Typography>
      <Typography variant="body2" color="text.secondary" noWrap>{product.brandName}</Typography>
      <Typography variant="h6" color="primary" mt={1}>${product.price}</Typography></CardContent>
    <CardActions><Button size="small" onClick={(e) => { e.stopPropagation(); addItem({ productId: product.id, productName: product.name, price: product.price, quantity: 1, imageUrl: product.imageUrls?.[0] || '' }) }}>Add to Cart</Button></CardActions>
  </Card>
}
