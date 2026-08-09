import { Box, Typography, Grid, Container } from '@mui/material'; import { useQuery } from '@tanstack/react-query'; import { productApi } from '../api/productApi'; import ProductCard from '../components/shared/ProductCard'; import Loading from '../components/shared/Loading'
export default function Home() {
  const { data, isLoading } = useQuery({ queryKey: ['featured'], queryFn: () => productApi.getFeatured({ page: 0, size: 8 }) })
  return <Box><Box sx={{ bgcolor: '#1976d2', color: 'white', py: 8, textAlign: 'center' }}><Container><Typography variant="h3" fontWeight={700}>Welcome to CommerceSphere</Typography><Typography variant="h6" mt={2}>Discover amazing products at great prices</Typography></Container></Box>
    <Container sx={{ mt: 4 }}><Typography variant="h5" mb={3}>Featured Products</Typography>{isLoading ? <Loading /> : <Grid container spacing={3}>{data?.content?.map((p: any) => <Grid item xs={12} sm={6} md={3} key={p.id}><ProductCard product={p} /></Grid>)}</Grid>}</Container></Box>
}
