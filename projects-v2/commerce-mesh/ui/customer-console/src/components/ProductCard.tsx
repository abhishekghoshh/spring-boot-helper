import { useNavigate } from 'react-router-dom';
import {
  Card,
  CardMedia,
  CardContent,
  CardActions,
  Typography,
  IconButton,
  Button,
  Rating,
  Box,
  Chip,
} from '@mui/material';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import FavoriteIcon from '@mui/icons-material/Favorite';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';

interface ProductCardProps {
  product: {
    id: string;
    name: string;
    images?: string[];
    image?: string;
    price: number;
    compareAtPrice?: number;
    rating?: number;
    reviewCount?: number;
    category?: string;
    inStock?: boolean;
  };
  isInWishlist?: boolean;
  onAddToCart: () => void;
  onToggleWishlist: () => void;
}

export default function ProductCard({ product, isInWishlist = false, onAddToCart, onToggleWishlist }: ProductCardProps) {
  const navigate = useNavigate();
  const imageSrc = product.images?.[0] || product.image || `https://placehold.co/300x300/1976d2/white?text=${encodeURIComponent(product.name)}`;
  const discount = product.compareAtPrice
    ? Math.round((1 - product.price / product.compareAtPrice) * 100)
    : 0;

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        transition: 'box-shadow 0.3s',
        '&:hover': { boxShadow: 6 },
        position: 'relative',
      }}
    >
      {discount > 0 && (
        <Chip
          label={`-${discount}%`}
          color="error"
          size="small"
          sx={{ position: 'absolute', top: 8, left: 8, zIndex: 1, fontWeight: 700 }}
        />
      )}

      <Box sx={{ position: 'relative' }}>
        <CardMedia
          component="img"
          height="220"
          image={imageSrc}
          alt={product.name}
          sx={{ cursor: 'pointer', objectFit: 'cover' }}
          onClick={() => navigate(`/products/${product.id}`)}
        />
        <IconButton
          onClick={(e) => {
            e.stopPropagation();
            onToggleWishlist();
          }}
          sx={{
            position: 'absolute',
            top: 4,
            right: 4,
            bgcolor: 'rgba(255,255,255,0.85)',
            '&:hover': { bgcolor: 'rgba(255,255,255,0.95)' },
          }}
          size="small"
          aria-label={isInWishlist ? 'Remove from wishlist' : 'Add to wishlist'}
        >
          {isInWishlist ? (
            <FavoriteIcon fontSize="small" color="error" />
          ) : (
            <FavoriteBorderIcon fontSize="small" />
          )}
        </IconButton>
      </Box>

      <CardContent sx={{ flexGrow: 1, pb: 0 }}>
        {product.category && (
          <Typography variant="caption" color="text.secondary" textTransform="uppercase" gutterBottom>
            {product.category}
          </Typography>
        )}
        <Typography
          variant="subtitle1"
          fontWeight={600}
          gutterBottom
          sx={{
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            cursor: 'pointer',
          }}
          onClick={() => navigate(`/products/${product.id}`)}
        >
          {product.name}
        </Typography>

        {product.rating !== undefined && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
            <Rating value={product.rating} precision={0.5} readOnly size="small" />
            {product.reviewCount !== undefined && (
              <Typography variant="caption" color="text.secondary">
                ({product.reviewCount})
              </Typography>
            )}
          </Box>
        )}

        <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1 }}>
          <Typography variant="h6" color="primary.main" fontWeight={700}>
            ${product.price.toFixed(2)}
          </Typography>
          {product.compareAtPrice && product.compareAtPrice > product.price && (
            <Typography
              variant="body2"
              color="text.disabled"
              sx={{ textDecoration: 'line-through' }}
            >
              ${product.compareAtPrice.toFixed(2)}
            </Typography>
          )}
        </Box>
      </CardContent>

      <CardActions sx={{ p: 2, pt: 1 }}>
        <Button
          variant="contained"
          fullWidth
          size="small"
          startIcon={<AddShoppingCartIcon />}
          onClick={(e) => {
            e.stopPropagation();
            onAddToCart();
          }}
          disabled={product.inStock === false}
        >
          {product.inStock === false ? 'Out of Stock' : 'Add to Cart'}
        </Button>
      </CardActions>
    </Card>
  );
}
