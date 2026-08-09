import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Grid,
  Chip,
  Button,
  Rating,
  IconButton,
  TextField,
  Skeleton,
  Divider,
  Paper,
  Avatar,
} from '@mui/material';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import FavoriteIcon from '@mui/icons-material/Favorite';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import { useProduct, useProductReviews } from '../hooks/useProducts';
import { useCart } from '../hooks/useCart';
import { useWishlist, useAddToWishlist, useRemoveFromWishlist } from '../hooks/useWishlist';
import { useAuth } from '../hooks/useAuth';
import { productApi } from '../api/productApi';
import { useQueryClient } from '@tanstack/react-query';

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { addItem } = useCart();
  const { data: wishlist = [] } = useWishlist();
  const addToWishlist = useAddToWishlist();
  const removeFromWishlist = useRemoveFromWishlist();
  const queryClient = useQueryClient();

  const { data: product, isLoading, isError } = useProduct(id || '');
  const { data: reviewsData } = useProductReviews(id || '');

  const [quantity, setQuantity] = useState(1);
  const [selectedImage, setSelectedImage] = useState(0);
  const [rating, setRating] = useState<number>(5);
  const [reviewTitle, setReviewTitle] = useState('');
  const [reviewComment, setReviewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  const isInWishlist = product ? wishlist.some((p) => p.id === product.id) : false;
  const reviews = Array.isArray(reviewsData) ? reviewsData : reviewsData?.data || [];

  const handleToggleWishlist = () => {
    if (!product) return;
    if (isInWishlist) {
      removeFromWishlist.mutate(product.id);
    } else {
      addToWishlist.mutate(product.id);
    }
  };

  const handleAddToCart = () => {
    if (!product) return;
    addItem({
      productId: product.id,
      name: product.name,
      image: product.images?.[0] || '',
      price: product.price,
      quantity,
    });
  };

  const handleSubmitReview = async () => {
    if (!id || !reviewComment.trim()) return;
    setSubmittingReview(true);
    try {
      await productApi.createReview(id, {
        rating,
        title: reviewTitle,
        comment: reviewComment,
      });
      setReviewTitle('');
      setReviewComment('');
      setRating(5);
      queryClient.invalidateQueries({ queryKey: ['reviews', id] });
    } catch {
      // ignore
    } finally {
      setSubmittingReview(false);
    }
  };

  if (isLoading) {
    return (
      <Box>
        <Grid container spacing={6}>
          <Grid item xs={12} md={6}>
            <Skeleton variant="rectangular" height={500} sx={{ borderRadius: 2 }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <Skeleton variant="text" width="30%" />
            <Skeleton variant="text" height={60} />
            <Skeleton variant="text" width="20%" />
            <Skeleton variant="rectangular" height={120} sx={{ mt: 2 }} />
            <Skeleton variant="rectangular" height={48} sx={{ mt: 3 }} width={200} />
          </Grid>
        </Grid>
      </Box>
    );
  }

  if (isError || !product) {
    return (
      <Box sx={{ textAlign: 'center', py: 12 }}>
        <Typography variant="h5" gutterBottom>
          Product not found
        </Typography>
        <Button variant="contained" onClick={() => navigate('/products')}>
          Browse Products
        </Button>
      </Box>
    );
  }

  const images = product.images?.length ? product.images : [product.images?.[0] || `https://placehold.co/600x600/1976d2/white?text=${encodeURIComponent(product.name)}`];

  return (
    <Box>
      <Grid container spacing={6}>
        {/* Image gallery */}
        <Grid item xs={12} md={6}>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {images.slice(0, 4).map((img, i) => (
                <Box
                  key={i}
                  component="img"
                  src={img}
                  alt={`${product.name} ${i + 1}`}
                  onClick={() => setSelectedImage(i)}
                  sx={{
                    width: 80,
                    height: 80,
                    borderRadius: 1,
                    objectFit: 'cover',
                    cursor: 'pointer',
                    border: 2,
                    borderColor: i === selectedImage ? 'primary.main' : 'transparent',
                    opacity: i === selectedImage ? 1 : 0.6,
                    transition: 'all 0.2s',
                  }}
                />
              ))}
            </Box>
            <Box
              component="img"
              src={images[selectedImage]}
              alt={product.name}
              sx={{
                width: '100%',
                maxHeight: 520,
                borderRadius: 2,
                objectFit: 'cover',
                flex: 1,
              }}
            />
          </Box>
        </Grid>

        {/* Product info */}
        <Grid item xs={12} md={6}>
          <Chip label={product.category} size="small" sx={{ mb: 1 }} />
          <Typography variant="h4" fontWeight={700} gutterBottom>
            {product.name}
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Brand: {product.brand}
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <Rating value={product.rating} precision={0.5} readOnly />
            <Typography variant="body2" color="text.secondary">
              ({product.reviewCount || 0} reviews)
            </Typography>
          </Box>

          <Box sx={{ mb: 3 }}>
            <Typography variant="h4" color="primary.main" fontWeight={700}>
              ${product.price.toFixed(2)}
              {product.compareAtPrice && product.compareAtPrice > product.price && (
                <Typography
                  component="span"
                  variant="h6"
                  color="text.disabled"
                  sx={{ textDecoration: 'line-through', ml: 1 }}
                >
                  ${product.compareAtPrice.toFixed(2)}
                </Typography>
              )}
            </Typography>
            {product.compareAtPrice && product.compareAtPrice > product.price && (
              <Chip
                label={`Save ${Math.round((1 - product.price / product.compareAtPrice) * 100)}%`}
                color="error"
                size="small"
                sx={{ mt: 0.5 }}
              />
            )}
          </Box>

          <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.8 }}>
            {product.description}
          </Typography>

          {product.attributes && Object.keys(product.attributes).length > 0 && (
            <Box sx={{ mb: 3 }}>
              {Object.entries(product.attributes).map(([key, value]) => (
                <Typography key={key} variant="body2">
                  <strong>{key}:</strong> {value}
                </Typography>
              ))}
            </Box>
          )}

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
            <Typography variant="body2" fontWeight={600}>
              Quantity:
            </Typography>
            <IconButton size="small" onClick={() => setQuantity(Math.max(1, quantity - 1))}>
              <RemoveIcon />
            </IconButton>
            <TextField
              value={quantity}
              onChange={(e) => {
                const v = parseInt(e.target.value, 10);
                if (!isNaN(v) && v >= 1) setQuantity(v);
              }}
              size="small"
              sx={{ width: 60 }}
              inputProps={{ style: { textAlign: 'center' }, min: 1 }}
            />
            <IconButton size="small" onClick={() => setQuantity(quantity + 1)}>
              <AddIcon />
            </IconButton>
          </Box>

          <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
            <Button
              variant="contained"
              size="large"
              startIcon={<AddShoppingCartIcon />}
              onClick={handleAddToCart}
              disabled={!product.inStock}
              sx={{ px: 4 }}
            >
              {product.inStock ? 'Add to Cart' : 'Out of Stock'}
            </Button>
            <IconButton
              onClick={handleToggleWishlist}
              color={isInWishlist ? 'error' : 'default'}
              sx={{ border: 1, borderColor: 'divider' }}
            >
              {isInWishlist ? <FavoriteIcon /> : <FavoriteBorderIcon />}
            </IconButton>
          </Box>
        </Grid>
      </Grid>

      {/* Reviews Section */}
      <Divider sx={{ my: 6 }} />
      <Typography variant="h5" fontWeight={700} gutterBottom>
        Customer Reviews
      </Typography>

      {reviews.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
          No reviews yet. Be the first to review this product!
        </Typography>
      ) : (
        <Grid container spacing={3} sx={{ mb: 4 }}>
          {reviews.map((review) => (
            <Grid item xs={12} key={review.id}>
              <Paper variant="outlined" sx={{ p: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                  <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36, fontSize: 14 }}>
                    {review.username?.charAt(0)?.toUpperCase() || 'U'}
                  </Avatar>
                  <Box>
                    <Typography variant="subtitle2">{review.username || 'Anonymous'}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {new Date(review.createdAt).toLocaleDateString()}
                    </Typography>
                  </Box>
                  <Rating value={review.rating} size="small" readOnly sx={{ ml: 'auto' }} />
                </Box>
                {review.title && (
                  <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                    {review.title}
                  </Typography>
                )}
                <Typography variant="body2" color="text.secondary">
                  {review.comment}
                </Typography>
              </Paper>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Add Review Form */}
      {isAuthenticated ? (
        <Paper variant="outlined" sx={{ p: 3, maxWidth: 600 }}>
          <Typography variant="h6" gutterBottom>
            Write a Review
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <Typography variant="body2">Your Rating:</Typography>
            <Rating value={rating} onChange={(_, v) => setRating(v || 5)} />
          </Box>
          <TextField
            fullWidth
            label="Review Title"
            size="small"
            value={reviewTitle}
            onChange={(e) => setReviewTitle(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            label="Your Review"
            multiline
            rows={4}
            value={reviewComment}
            onChange={(e) => setReviewComment(e.target.value)}
            sx={{ mb: 2 }}
          />
          <Button
            variant="contained"
            onClick={handleSubmitReview}
            disabled={submittingReview || !reviewComment.trim()}
          >
            {submittingReview ? 'Submitting...' : 'Submit Review'}
          </Button>
        </Paper>
      ) : (
        <Paper variant="outlined" sx={{ p: 3, maxWidth: 600, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            Please{' '}
            <Button component="a" href="/login" size="small" sx={{ textTransform: 'none' }}>
              sign in
            </Button>{' '}
            to write a review.
          </Typography>
        </Paper>
      )}
    </Box>
  );
}
