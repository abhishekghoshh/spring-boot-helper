import { Box, Typography, Button, TextField, Card, CardContent, Alert } from '@mui/material'
import { useState } from 'react'
import { inventoryApi } from '../api/inventoryApi'

export default function Inventory() {
  const [productId, setProductId] = useState('')
  const [warehouseId, setWarehouseId] = useState('')
  const [productName, setProductName] = useState('')
  const [quantity, setQuantity] = useState(0)
  const [result, setResult] = useState<any>(null)
  const [msg, setMsg] = useState('')

  const handleAddStock = async () => {
    try {
      const r = await inventoryApi.addStock(productId, warehouseId, productName, quantity)
      setResult(r); setMsg(`Stock updated! Available: ${r.availableQuantity}`)
    } catch { setMsg('Failed to add stock') }
  }

  return (
    <Box>
      <Typography variant="h4" mb={3}>Inventory Management</Typography>
      {msg && <Alert severity="info" sx={{ mb: 2 }}>{msg}</Alert>}
      <Card><CardContent>
        <TextField label="Product ID" fullWidth value={productId} onChange={e => setProductId(e.target.value)} margin="normal" />
        <TextField label="Warehouse ID" fullWidth value={warehouseId} onChange={e => setWarehouseId(e.target.value)} margin="normal" />
        <TextField label="Product Name" fullWidth value={productName} onChange={e => setProductName(e.target.value)} margin="normal" />
        <TextField label="Quantity" fullWidth type="number" value={quantity} onChange={e => setQuantity(Number(e.target.value))} margin="normal" />
        <Button variant="contained" onClick={handleAddStock} sx={{ mt: 2 }}>Add Stock</Button>
      </CardContent></Card>
    </Box>
  )
}
