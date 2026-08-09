import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Button,
  TextField,
  Divider,
  Stack,
  Alert,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import { useCart } from '../hooks/useCart';
import { useAuth } from '../hooks/useAuth';

export default function CartPage() {
  const { items, total, subtotal, discount, couponCode, couponDiscount, updateQuantity, removeItem, clearCart, applyCoupon } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [couponInput, setCouponInput] = useState('');
  const [couponError, setCouponError] = useState('');
  const [applyingCoupon, setApplyingCoupon] = useState(false);

  const handleApplyCoupon = async () => {
    if (!couponInput.trim()) return;
    setApplyingCoupon(true);
    setCouponError('');
    try {
      await applyCoupon(couponInput.trim());
      setCouponInput('');
    } catch {
      setCouponError('Invalid coupon code');
    } finally {
      setApplyingCoupon(false);
    }
  };

  if (items.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 12 }}>
        <Typography variant="h5" gutterBottom>
          Your cart is empty
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          Looks like you haven't added any products to your cart yet.
        </Typography>
        <Button variant="contained" size="large" component={Link} to="/products">
          Continue Shopping
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Shopping Cart
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {items.length} item{items.length !== 1 ? 's' : ''} in your cart
      </Typography>

      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={4} alignItems="flex-start">
        {/* Cart Items Table */}
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <TableContainer component={Paper} variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Product</TableCell>
                  <TableCell align="right">Price</TableCell>
                  <TableCell align="center">Quantity</TableCell>
                  <TableCell align="right">Subtotal</TableCell>
                  <TableCell width={60} />
                </TableRow>
              </TableHead>
              <TableBody>
                {items.map((item) => (
                  <TableRow key={item.productId}>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Box
                          component="img"
                          src={item.image || `https://placehold.co/80x80/1976d2/white?text=X`}
                          alt={item.name}
                          sx={{ width: 70, height: 70, borderRadius: 1, objectFit: 'cover' }}
                        />
                        <Box>
                          <Typography
                            variant="subtitle2"
                            fontWeight={600}
                            component={Link}
                            to={`/products/${item.productId}`}
                            sx={{ textDecoration: 'none', color: 'text.primary', '&:hover': { color: 'primary.main' } }}
                          >
                            {item.name}
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell align="right">${item.price.toFixed(2)}</TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <IconButton size="small" onClick={() => updateQuantity(item.productId, item.quantity - 1)}>
                          <RemoveIcon fontSize="small" />
                        </IconButton>
                        <Typography sx={{ mx: 1.5, minWidth: 24, textAlign: 'center' }}>
                          {item.quantity}
                        </Typography>
                        <IconButton size="small" onClick={() => updateQuantity(item.productId, item.quantity + 1)}>
                          <AddIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }}>
                      ${(item.price * item.quantity).toFixed(2)}
                    </TableCell>
                    <TableCell>
                      <IconButton color="error" size="small" onClick={() => removeItem(item.productId)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          <Box sx={{ mt: 2 }}>
            <Button color="error" onClick={clearCart} size="small">
              Clear Cart
            </Button>
          </Box>
        </Box>

        {/* Order Summary Sidebar */}
        <Paper variant="outlined" sx={{ width: { xs: '100%', lg: 340 }, p: 3, flexShrink: 0 }}>
          <Typography variant="h6" fontWeight={600} gutterBottom>
            Order Summary
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Subtotal
            </Typography>
            <Typography variant="body2">${subtotal.toFixed(2)}</Typography>
          </Box>

          {couponDiscount > 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="body2" color="success.main">
                Coupon ({couponCode})
              </Typography>
              <Typography variant="body2" color="success.main">
                -${couponDiscount.toFixed(2)}
              </Typography>
            </Box>
          )}

          {discount > 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="body2" color="text.secondary">
                Discount
              </Typography>
              <Typography variant="body2">-${discount.toFixed(2)}</Typography>
            </Box>
          )}

          <Divider sx={{ my: 2 }} />

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
            <Typography variant="h6" fontWeight={700}>
              Total
            </Typography>
            <Typography variant="h6" fontWeight={700} color="primary.main">
              ${total.toFixed(2)}
            </Typography>
          </Box>

          {/* Coupon */}
          <Typography variant="body2" fontWeight={600} gutterBottom>
            Coupon Code
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <TextField
              size="small"
              placeholder="Enter code"
              value={couponInput}
              onChange={(e) => setCouponInput(e.target.value)}
              sx={{ flex: 1 }}
            />
            <Button
              variant="outlined"
              onClick={handleApplyCoupon}
              disabled={applyingCoupon || !couponInput.trim()}
              size="small"
            >
              {applyingCoupon ? '...' : 'Apply'}
            </Button>
          </Box>
          {couponError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {couponError}
            </Alert>
          )}

          <Button
            variant="contained"
            fullWidth
            size="large"
            onClick={() => navigate(isAuthenticated ? '/checkout' : '/login?redirect=/checkout')}
            sx={{ fontWeight: 600 }}
          >
            Proceed to Checkout
          </Button>

          <Button
            fullWidth
            size="small"
            component={Link}
            to="/products"
            sx={{ mt: 1, textTransform: 'none' }}
          >
            Continue Shopping
          </Button>
        </Paper>
      </Stack>
    </Box>
  );
}
