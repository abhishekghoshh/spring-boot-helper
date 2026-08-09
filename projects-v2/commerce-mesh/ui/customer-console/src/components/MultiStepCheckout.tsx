import { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  Stepper,
  Step,
  StepLabel,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCart } from '../hooks/useCart';

const shippingSchema = z.object({
  fullName: z.string().min(2, 'Name is required'),
  street: z.string().min(5, 'Address is required'),
  city: z.string().min(2, 'City is required'),
  state: z.string().min(2, 'State is required'),
  postalCode: z.string().min(4, 'Postal code is required'),
  phone: z.string().min(7, 'Phone is required'),
});

const paymentSchema = z.object({
  cardNumber: z.string().regex(/^\d{16}$/, 'Enter 16-digit card number'),
  expiry: z.string().regex(/^\d{2}\/\d{2}$/, 'Use MM/YY format'),
  cvv: z.string().regex(/^\d{3,4}$/, 'Enter 3 or 4 digit CVV'),
  cardName: z.string().min(2, 'Cardholder name is required'),
});

type ShippingForm = z.infer<typeof shippingSchema>;
type PaymentForm = z.infer<typeof paymentSchema>;

const STEPS = ['Shipping Address', 'Payment', 'Review & Place Order'];

interface MultiStepCheckoutProps {
  onComplete: (data: { shippingAddress: ShippingForm; paymentMethod: string }) => void;
  isSubmitting?: boolean;
}

export default function MultiStepCheckout({ onComplete, isSubmitting = false }: MultiStepCheckoutProps) {
  const [activeStep, setActiveStep] = useState(0);
  const { items, subtotal, discount, total } = useCart();

  const shippingForm = useForm<ShippingForm>({
    resolver: zodResolver(shippingSchema),
    defaultValues: { fullName: '', street: '', city: '', state: '', postalCode: '', phone: '' },
  });

  const paymentForm = useForm<PaymentForm>({
    resolver: zodResolver(paymentSchema),
    defaultValues: { cardNumber: '', expiry: '', cvv: '', cardName: '' },
  });

  const handleShippingNext = async () => {
    const valid = await shippingForm.trigger();
    if (valid) setActiveStep(1);
  };

  const handlePaymentNext = async () => {
    const valid = await paymentForm.trigger();
    if (valid) setActiveStep(2);
  };

  const handlePlaceOrder = () => {
    const shipping = shippingForm.getValues();
    const payment = paymentForm.getValues();
    onComplete({
      shippingAddress: shipping,
      paymentMethod: `Card ending in ${payment.cardNumber.slice(-4)}`,
    });
  };

  return (
    <Box>
      <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
        {STEPS.map((label, idx) => (
          <Step key={label} completed={activeStep > idx}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {/* Step 0: Shipping */}
      {activeStep === 0 && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Shipping Address
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Controller
                name="fullName"
                control={shippingForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Full Name"
                    size="small"
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="street"
                control={shippingForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Street Address"
                    size="small"
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="city"
                control={shippingForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="City"
                    size="small"
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="state"
                control={shippingForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="State"
                    size="small"
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="postalCode"
                control={shippingForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Postal Code"
                    size="small"
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Controller
                name="phone"
                control={shippingForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Phone"
                    size="small"
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Button variant="contained" onClick={handleShippingNext} sx={{ mt: 1 }}>
                Continue to Payment
              </Button>
            </Grid>
          </Grid>
        </Paper>
      )}

      {/* Step 1: Payment */}
      {activeStep === 1 && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Payment Method
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Controller
                name="cardName"
                control={paymentForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Cardholder Name"
                    size="small"
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="cardNumber"
                control={paymentForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Card Number"
                    size="small"
                    placeholder="1234 5678 9012 3456"
                    inputProps={{ maxLength: 16 }}
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={6}>
              <Controller
                name="expiry"
                control={paymentForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="Expiry (MM/YY)"
                    size="small"
                    placeholder="MM/YY"
                    inputProps={{ maxLength: 5 }}
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={6}>
              <Controller
                name="cvv"
                control={paymentForm.control}
                render={({ field, fieldState }) => (
                  <TextField
                    {...field}
                    fullWidth
                    label="CVV"
                    size="small"
                    type="password"
                    inputProps={{ maxLength: 4 }}
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2, mt: 1 }}>
                <Button variant="outlined" onClick={() => setActiveStep(0)}>
                  Back
                </Button>
                <Button variant="contained" onClick={handlePaymentNext}>
                  Review Order
                </Button>
              </Box>
            </Grid>
          </Grid>
        </Paper>
      )}

      {/* Step 2: Review */}
      {activeStep === 2 && (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Review Your Order
          </Typography>

          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Shipping To:
            </Typography>
            <Typography variant="body2">
              {shippingForm.getValues().fullName}
            </Typography>
            <Typography variant="body2">
              {shippingForm.getValues().street}
            </Typography>
            <Typography variant="body2">
              {shippingForm.getValues().city}, {shippingForm.getValues().state}{' '}
              {shippingForm.getValues().postalCode}
            </Typography>
            <Typography variant="body2">{shippingForm.getValues().phone}</Typography>
          </Box>

          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Payment:
            </Typography>
            <Typography variant="body2">
              Card ending in {paymentForm.getValues().cardNumber.slice(-4)}
            </Typography>
          </Box>

          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Items ({items.length}):
            </Typography>
            {items.map((item) => (
              <Box
                key={item.productId}
                sx={{ display: 'flex', justifyContent: 'space-between', py: 0.5 }}
              >
                <Typography variant="body2">
                  {item.name} × {item.quantity}
                </Typography>
                <Typography variant="body2">
                  ${(item.price * item.quantity).toFixed(2)}
                </Typography>
              </Box>
            ))}
          </Box>

          <Box
            sx={{
              borderTop: 1,
              borderColor: 'divider',
              pt: 2,
              display: 'flex',
              flexDirection: 'column',
              gap: 0.5,
            }}
          >
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2">Subtotal</Typography>
              <Typography variant="body2">${subtotal.toFixed(2)}</Typography>
            </Box>
            {discount > 0 && (
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="success.main">
                  Discount
                </Typography>
                <Typography variant="body2" color="success.main">
                  -${discount.toFixed(2)}
                </Typography>
              </Box>
            )}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
              <Typography variant="h6" fontWeight={700}>
                Total
              </Typography>
              <Typography variant="h6" fontWeight={700} color="primary.main">
                ${total.toFixed(2)}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
            <Button variant="outlined" onClick={() => setActiveStep(1)}>
              Back
            </Button>
            <Button
              variant="contained"
              color="success"
              size="large"
              onClick={handlePlaceOrder}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Placing Order...' : 'Place Order'}
            </Button>
          </Box>
        </Paper>
      )}
    </Box>
  );
}
