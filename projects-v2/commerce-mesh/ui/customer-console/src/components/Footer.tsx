import { Box, Container, Typography, Link as MuiLink, Stack } from '@mui/material';

export default function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        bgcolor: 'grey.900',
        color: 'grey.300',
        py: 6,
        mt: 'auto',
      }}
    >
      <Container maxWidth="xl">
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={4}
          justifyContent="space-between"
          alignItems={{ xs: 'center', sm: 'flex-start' }}
        >
          <Box>
            <Typography variant="h6" color="white" fontWeight={700} gutterBottom>
              CommerceMesh
            </Typography>
            <Typography variant="body2" color="grey.400" sx={{ maxWidth: 300 }}>
              Your one-stop destination for amazing products at unbeatable prices.
            </Typography>
          </Box>

          <Box>
            <Typography variant="subtitle2" color="white" fontWeight={600} gutterBottom>
              Shop
            </Typography>
            <Stack spacing={0.5}>
              {['Electronics', 'Fashion', 'Home & Garden', 'Sports', 'Books'].map((cat) => (
                <MuiLink key={cat} href={`/products?category=${encodeURIComponent(cat)}`} color="grey.400" underline="hover" variant="body2">
                  {cat}
                </MuiLink>
              ))}
            </Stack>
          </Box>

          <Box>
            <Typography variant="subtitle2" color="white" fontWeight={600} gutterBottom>
              Customer Service
            </Typography>
            <Stack spacing={0.5}>
              <MuiLink href="#" color="grey.400" underline="hover" variant="body2">Contact Us</MuiLink>
              <MuiLink href="#" color="grey.400" underline="hover" variant="body2">Shipping Info</MuiLink>
              <MuiLink href="#" color="grey.400" underline="hover" variant="body2">Returns</MuiLink>
              <MuiLink href="#" color="grey.400" underline="hover" variant="body2">FAQ</MuiLink>
            </Stack>
          </Box>

          <Box>
            <Typography variant="subtitle2" color="white" fontWeight={600} gutterBottom>
              Account
            </Typography>
            <Stack spacing={0.5}>
              <MuiLink href="/profile" color="grey.400" underline="hover" variant="body2">My Profile</MuiLink>
              <MuiLink href="/orders" color="grey.400" underline="hover" variant="body2">Order History</MuiLink>
              <MuiLink href="/wishlist" color="grey.400" underline="hover" variant="body2">Wishlist</MuiLink>
            </Stack>
          </Box>
        </Stack>

        <Box sx={{ borderTop: 1, borderColor: 'grey.800', mt: 4, pt: 3, textAlign: 'center' }}>
          <Typography variant="body2" color="grey.500">
            © {new Date().getFullYear()} CommerceMesh. All rights reserved.
          </Typography>
        </Box>
      </Container>
    </Box>
  );
}
