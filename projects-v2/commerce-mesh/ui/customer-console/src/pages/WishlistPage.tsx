import { Box, Typography, Grid, IconButton, Button } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useNavigate } from 'react-router-dom';
import { useWishlist, useRemoveFromWishlist } from '../hooks/useWishlist';
import { useCart } from '../hooks/useCart';
import ProductCard from '../components/ProductCard';
import { Skeleton } from '@mui/material';

export default function WishlistPage() {
  const navigate = useNavigate();
  const { data: wishlist = [], isLoading, isError } = useWishlist();
  const removeFromWishlist = useRemoveFromWishlist();
  const { addItem } = useCart();

  if (isLoading) {
    return (
      <Box>
        <Typography variant="h4" fontWeight={700} gutterBottom>
          My Wishlist
        </Typography>
        <Grid container spacing={3}>
          {Array.from({ length: 4 }).map((_, i) => (
            <Grid item xs={12} sm={6} md={4} key={i}>
              <Skeleton variant="rectangular" height={250} sx={{ borderRadius: 2 }} />
            </Grid>
          ))}
        </Grid>
      </Box>
    );
  }

  if (isError) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="error" gutterBottom>
          Failed to load wishlist
        </Typography>
        <Button variant="outlined" onClick={() => window.location.reload()}>
          Try Again
        </Button>
      </Box>
    );
  }

  if (wishlist.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 12 }}>
        <Typography variant="h5" gutterBottom>
          Your wishlist is empty
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          Save items you love by clicking the heart icon on any product.
        </Typography>
        <Button variant="contained" size="large" onClick={() => navigate('/products')}>
          Browse Products
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" fontWeight={700}>
          My Wishlist
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {wishlist.length} item{wishlist.length !== 1 ? 's' : ''}
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {wishlist.map((product) => (
          <Grid item xs={12} sm={6} md={4} key={product.id}>
            <ProductCard
              product={product}
              isInWishlist={true}
              onAddToCart={() =>
                addItem({
                  productId: product.id,
                  name: product.name,
                  image: product.images?.[0] || '',
                  price: product.price,
                })
              }
              onToggleWishlist={() => removeFromWishlist.mutate(product.id)}
            />
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
