import { useQuery } from '@tanstack/react-query'
import { Grid, Typography } from '@mui/material'
import InventoryIcon from '@mui/icons-material/Inventory'
import CategoryIcon from '@mui/icons-material/Category'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import PeopleIcon from '@mui/icons-material/People'
import StatCard from '../components/dashboard/StatCard'
import { productApi } from '../api/productApi'
import { categoryApi, brandApi } from '../api/categoryApi'
import { authApi } from '../api/authApi'

export default function Dashboard() {
  const { data: products } = useQuery({ queryKey: ['products', 'count'], queryFn: () => productApi.search({ size: 1 }) })
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: categoryApi.list })
  const { data: brands } = useQuery({ queryKey: ['brands'], queryFn: brandApi.list })
  const { data: users } = useQuery({ queryKey: ['users'], queryFn: () => authApi.listUsers().catch(() => []) })

  return (
    <>
      <Typography variant="h4" mb={3}>Dashboard</Typography>
      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Total Products" value={products?.totalElements || 0} icon={<InventoryIcon fontSize="large" />} color="#1976d2" /></Grid>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Categories" value={categories?.length || 0} icon={<CategoryIcon fontSize="large" />} color="#388e3c" /></Grid>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Brands" value={brands?.length || 0} icon={<ShoppingCartIcon fontSize="large" />} color="#f57c00" /></Grid>
        <Grid item xs={12} sm={6} md={3}><StatCard title="Active Users" value={users?.length || 0} icon={<PeopleIcon fontSize="large" />} color="#7b1fa2" /></Grid>
      </Grid>
    </>
  )
}
