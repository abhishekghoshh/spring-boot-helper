import { Box, Typography, Grid, Button, Skeleton, Card, CardContent, CardActionArea } from '@mui/material';
import { Link } from 'react-router-dom';
import { useFeaturedProducts, useCategories } from '../hooks/useProducts';
import { useCart } from '../hooks/useCart';
import { useAddToWishlist, useRemoveFromWishlist, useWishlist } from '../hooks/useWishlist';
import ProductCard from '../components/ProductCard';

export default function HomePage() {
  const { data: featuredProducts, isLoading: featuredLoading } = useFeaturedProducts();
  const { data: categories, isLoading: catLoading } = useCategories();
  const { addItem } = useCart();
  const { data: wishlist = [] } = useWishlist();
  const addToWishlist = useAddToWishlist();
  const removeFromWishlist = useRemoveFromWishlist();

  const wishlistIds = new Set(wishlist.map((p) => p.id));

  const handleToggleWishlist = (productId: string) => {
    if (wishlistIds.has(productId)) {
      removeFromWishlist.mutate(productId);
    } else {
      addToWishlist.mutate(productId);
    }
  };

  return (
    <Box>
      {/* Hero */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 50%, #90caf9 100%)',
          color: 'white',
          py: { xs: 8, md: 12 },
          px: 3,
          borderRadius: 3,
          textAlign: 'center',
          mb: 8,
        }}
      >
        <Typography variant="h3" fontWeight={800} gutterBottom>
          Welcome to CommerceMesh
        </Typography>
        <Typography variant="h6" sx={{ opacity: 0.9, maxWidth: 600, mx: 'auto', mb: 1 }}>
          Discover amazing products at unbeatable prices. Fast shipping, easy returns.
        </Typography>
        <Button
          component={Link}
          to="/products"
          variant="contained"
          size="large"
          sx={{
            mt: 4,
            px: 8,
            py: 1.5,
            bgcolor: 'white',
            color: 'primary.main',
            fontWeight: 700,
            '&:hover': { bgcolor: 'grey.100' },
          }}
        >
          Shop Now
        </Button>
      </Box>

      {/* Categories */}
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Shop by Category
      </Typography>
      <Grid container spacing={2} sx={{ mb: 8 }}>
        {catLoading
          ? Array.from({ length: 6 }).map((_, i) => (
              <Grid item xs={6} sm={4} md={2} key={i}>
                <Skeleton variant="rectangular" height={120} sx={{ borderRadius: 2 }} />
              </Grid>
            ))
          : (categories || []).slice(0, 6).map((cat) => (
              <Grid item xs={6} sm={4} md={2} key={cat.id}>
                <Card
                  component={Link}
                  to={`/products?category=${encodeURIComponent(cat.name)}`}
                  sx={{ textAlign: 'center', textDecoration: 'none', transition: '0.2s', '&:hover': { boxShadow: 4 } }}
                >
                  <CardActionArea sx={{ p: 3 }}>
                    <Typography variant="h3" sx={{ mb: 1 }}>
                      {cat.name === 'Electronics' ? '🖥️' : cat.name === 'Fashion' ? '👗' : cat.name === 'Home & Garden' ? '🏡' : cat.name === 'Sports' ? '⚽' : cat.name === 'Books' ? '📚' : '🎮'}
                    </Typography>
                    <Typography variant="subtitle1" color="text.secondary" fontWeight={600}>
                      {cat.name}
                    </Typography>
                    {cat.productCount !== undefined && (
                      <Typography variant="caption" color="text.secondary">
                        {cat.productCount} products
                      </Typography>
                    )}
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
      </Grid>

      {/* Featured Products */}
      <Typography variant="h5" fontWeight={700} gutterBottom sx={{ mb: 3 }}>
        Featured Products
      </Typography>
      <Grid container spacing={3}>
        {featuredLoading
          ? Array.from({ length: 4 }).map((_, i) => (
              <Grid item xs={12} sm={6} md={3} key={i}>
                <Skeleton variant="rectangular" height={300} sx={{ borderRadius: 2 }} />
                <Skeleton variant="text" width="80%" />
                <Skeleton variant="text" width="40%" />
              </Grid>
            ))
          : (featuredProducts || []).map((product) => (
              <Grid item xs={12} sm={6} md={3} key={product.id}>
                <ProductCard
                  product={product}
                  isInWishlist={wishlistIds.has(product.id)}
                  onAddToCart={() =>
                    addItem({
                      productId: product.id,
                      name: product.name,
                      image: product.images?.[0] || '',
                      price: product.price,
                    })
                  }
                  onToggleWishlist={() => handleToggleWishlist(product.id)}
                />
              </Grid>
            ))}
      </Grid>
    </Box>
  );
}
