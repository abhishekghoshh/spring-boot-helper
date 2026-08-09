import { Outlet } from 'react-router-dom';
import { Box, Container } from '@mui/material';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';

export default function CustomerLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <Box component="main" sx={{ flex: 1 }}>
        <Container maxWidth="xl" sx={{ mt: 4, mb: 8 }}>
          <Outlet />
        </Container>
      </Box>
      <Footer />
    </Box>
  );
}
