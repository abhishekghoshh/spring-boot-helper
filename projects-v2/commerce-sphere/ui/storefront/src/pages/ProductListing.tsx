import { Box, Typography, Grid, Container } from '@mui/material'; import { useQuery } from '@tanstack/react-query'; import { productApi } from '../api/productApi'; import ProductCard from '../components/shared/ProductCard'; import Loading from '../components/shared/Loading'
export default function ProductListing() {
  const { data, isLoading } = useQuery({ queryKey: ['all-products'], queryFn: () => productApi.search({ page: 0, size: 20, active: true }) })
  return <Container sx={{ mt: 4 }}><Typography variant="h4" mb={3}>All Products</Typography>{isLoading ? <Loading /> : <Grid container spacing={3}>{data?.content?.map((p: any) => <Grid item xs={12} sm={6} md={3} key={p.id}><ProductCard product={p} /></Grid>)}</Grid>}</Container>
}
