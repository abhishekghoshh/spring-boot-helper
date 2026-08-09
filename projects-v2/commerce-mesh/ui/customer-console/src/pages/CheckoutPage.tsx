import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Alert, Snackbar } from '@mui/material';
import MultiStepCheckout from '../components/MultiStepCheckout';
import { useCreateOrder } from '../hooks/useOrders';
import { useCart } from '../hooks/useCart';
import type { Address } from '../types';

export default function CheckoutPage() {
  const navigate = useNavigate();
  const { items, clearCart } = useCart();
  const createOrder = useCreateOrder();
  const [errorMsg, setErrorMsg] = useState('');
  const [successOpen, setSuccessOpen] = useState(false);

  const handleComplete = async (data: { shippingAddress: Address; paymentMethod: string }) => {
    try {
      setErrorMsg('');
      await createOrder.mutateAsync({
        shippingAddress: {
          fullName: data.shippingAddress.fullName,
          street: data.shippingAddress.street,
          city: data.shippingAddress.city,
          state: data.shippingAddress.state,
          postalCode: data.shippingAddress.postalCode,
          phone: data.shippingAddress.phone,
          isDefault: true,
        },
        paymentMethod: data.paymentMethod,
      });
      clearCart();
      setSuccessOpen(true);
      setTimeout(() => navigate('/orders'), 1500);
    } catch {
      setErrorMsg('Failed to place order. Please try again.');
    }
  };

  if (items.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 12 }}>
        <Typography variant="h5" gutterBottom>
          No items to checkout
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Please add some products to your cart first.
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Checkout
      </Typography>

      {errorMsg && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setErrorMsg('')}>
          {errorMsg}
        </Alert>
      )}

      <MultiStepCheckout onComplete={handleComplete} isSubmitting={createOrder.isPending} />

      <Snackbar
        open={successOpen}
        autoHideDuration={3000}
        onClose={() => setSuccessOpen(false)}
        message="Order placed successfully! Redirecting..."
      />
    </Box>
  );
}
