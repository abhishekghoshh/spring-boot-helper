import { useParams } from 'react-router-dom'
import { Box, Typography, Card, CardContent, Chip, Table, TableBody, TableCell, TableRow, CircularProgress } from '@mui/material'
import { useOrder } from '../hooks/useOrders'

export default function OrderDetail() {
  const { id } = useParams()
  const { data: order, isLoading } = useOrder(id || '')

  if (isLoading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress /></Box>
  if (!order) return <Typography>Order not found</Typography>

  return (
    <Box>
      <Typography variant="h4" mb={3}>Order {order.orderNumber}</Typography>
      <Card><CardContent>
        <Chip label={order.status} color={order.status === 'CONFIRMED' ? 'success' : 'error'} sx={{ mb: 2 }} />
        <Table><TableBody>
          <TableRow><TableCell>Customer</TableCell><TableCell>{order.userEmail}</TableCell></TableRow>
          <TableRow><TableCell>Subtotal</TableCell><TableCell>${order.subtotal}</TableCell></TableRow>
          <TableRow><TableCell>Tax</TableCell><TableCell>${order.tax}</TableCell></TableRow>
          <TableRow><TableCell>Shipping</TableCell><TableCell>${order.shipping}</TableCell></TableRow>
          <TableRow><TableCell><strong>Total</strong></TableCell><TableCell><strong>${order.total}</strong></TableCell></TableRow>
          <TableRow><TableCell>Address</TableCell><TableCell>{order.shippingAddress}</TableCell></TableRow>
        </TableBody></Table>
        <Typography variant="h6" mt={2}>Items</Typography>
        {order.items?.map((i: any) => (
          <Typography key={i.productId}>{i.productName} x{i.quantity} @ ${i.price}</Typography>
        ))}
      </CardContent></Card>
    </Box>
  )
}
