import { useParams, useNavigate } from 'react-router-dom'
import { Box, Button, Card, CardContent, TextField, Typography, Switch, FormControlLabel, MenuItem } from '@mui/material'
import { useProduct, useCreateProduct, useUpdateProduct } from '../hooks/useProducts'
import { useCategories, useBrands } from '../hooks/useCategories'
import { useState, useEffect } from 'react'
import Loading from '../components/shared/Loading'

export default function ProductForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = !!id
  const { data: product, isLoading } = useProduct(id || '')
  const { data: categories } = useCategories()
  const { data: brands } = useBrands()
  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct()

  const [form, setForm] = useState({ name: '', sku: '', description: '', price: '', currency: 'USD', categoryId: '', brandId: '', active: true, featured: false })

  useEffect(() => { if (product) setForm({ name: product.name, sku: product.sku, description: product.description, price: product.price, currency: product.currency || 'USD', categoryId: product.categoryId, brandId: product.brandId, active: product.active, featured: product.featured }) }, [product])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [e.target.name]: e.target.value })
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const data = { ...form, price: parseFloat(form.price) }
    if (isEdit) await updateProduct.mutateAsync({ id: id!, data })
    else await createProduct.mutateAsync(data)
    navigate('/products')
  }

  if (isEdit && isLoading) return <Loading />
  return (
    <Box><Typography variant="h4" mb={3}>{isEdit ? 'Edit Product' : 'New Product'}</Typography>
      <Card><CardContent><form onSubmit={handleSubmit}>
        <TextField fullWidth label="Name" name="name" value={form.name} onChange={handleChange} required margin="normal" />
        <TextField fullWidth label="SKU" name="sku" value={form.sku} onChange={handleChange} required margin="normal" />
        <TextField fullWidth label="Description" name="description" value={form.description} onChange={handleChange} required multiline rows={3} margin="normal" />
        <TextField fullWidth label="Price" name="price" type="number" value={form.price} onChange={handleChange} required margin="normal" inputProps={{ step: 0.01 }} />
        <TextField fullWidth select label="Category" name="categoryId" value={form.categoryId} onChange={handleChange} required margin="normal">
          {categories?.map((c: any) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
        </TextField>
        <TextField fullWidth select label="Brand" name="brandId" value={form.brandId} onChange={handleChange} required margin="normal">
          {brands?.map((b: any) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
        </TextField>
        <FormControlLabel control={<Switch checked={form.active} onChange={e => setForm({ ...form, active: e.target.checked })} />} label="Active" />
        <FormControlLabel control={<Switch checked={form.featured} onChange={e => setForm({ ...form, featured: e.target.checked })} />} label="Featured" />
        <Box mt={2}><Button type="submit" variant="contained" size="large">{isEdit ? 'Update' : 'Create'} Product</Button></Box>
      </form></CardContent></Card></Box>
  )
}
