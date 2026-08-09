import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Box, Button, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, IconButton, TextField } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useProducts, useDeleteProduct } from '../hooks/useProducts'
import Loading from '../components/shared/Loading'

export default function Products() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const { data, isLoading } = useProducts({ page, size: 20, query })
  const deleteProduct = useDeleteProduct()

  return (
    <>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Products</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/products/new')}>Add Product</Button>
      </Box>
      <TextField label="Search" value={query} onChange={e => setQuery(e.target.value)} size="small" sx={{ mb: 2, width: 300 }} />
      {isLoading ? <Loading /> : (
        <TableContainer component={Paper}>
          <Table><TableHead><TableRow>
            <TableCell>Name</TableCell><TableCell>SKU</TableCell><TableCell>Price</TableCell><TableCell>Category</TableCell><TableCell>Status</TableCell><TableCell align="right">Actions</TableCell>
          </TableRow></TableHead>
          <TableBody>
            {data?.content?.map((p: any) => (
              <TableRow key={p.id}><TableCell>{p.name}</TableCell><TableCell>{p.sku}</TableCell><TableCell>${p.price}</TableCell><TableCell>{p.categoryName}</TableCell><TableCell>{p.active ? 'Active' : 'Inactive'}</TableCell>
                <TableCell align="right">
                  <IconButton onClick={() => navigate(`/products/${p.id}/edit`)}><EditIcon /></IconButton>
                  <IconButton color="error" onClick={() => { if (confirm('Delete?')) deleteProduct.mutate(p.id) }}><DeleteIcon /></IconButton>
                </TableCell></TableRow>
            ))}
          </TableBody></Table>
        </TableContainer>
      )}
    </>
  )
}
