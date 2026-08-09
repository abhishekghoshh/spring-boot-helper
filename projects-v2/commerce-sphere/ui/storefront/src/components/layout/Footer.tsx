import { Box, Typography, Container } from '@mui/material'
export default function Footer() {
  return <Box component="footer" sx={{ py: 3, mt: 8, bgcolor: '#f5f5f5', borderTop: '1px solid #e0e0e0' }}>
    <Container><Typography variant="body2" color="text.secondary" align="center">&copy; 2026 CommerceSphere. All rights reserved.</Typography></Container></Box>
}
