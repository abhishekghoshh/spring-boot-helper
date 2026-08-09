import { Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material'
import { useState, useEffect } from 'react'
import { orderApi } from '../api/inventoryApi'

export default function Orders() {
  const [orders, setOrders] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    orderApi.listByUser('all', 0, 50).catch(() => []).finally(() => setLoading(false))
  }, [])

  return (
    <Box>
      <Typography variant="h4" mb={3}>Orders</Typography>
      <TableContainer component={Paper}>
        <Table><TableHead><TableRow><TableCell>Order #</TableCell><TableCell>User</TableCell><TableCell>Total</TableCell><TableCell>Status</TableCell><TableCell>Date</TableCell></TableRow></TableHead>
          <TableBody>{orders.map((o: any) => (
            <TableRow key={o.id}><TableCell>{o.orderNumber}</TableCell><TableCell>{o.userEmail}</TableCell><TableCell>${o.total}</TableCell><TableCell>{o.status}</TableCell><TableCell>{new Date(o.createdAt).toLocaleDateString()}</TableCell></TableRow>
          ))}</TableBody></Table>
      </TableContainer>
    </Box>
  )
}
