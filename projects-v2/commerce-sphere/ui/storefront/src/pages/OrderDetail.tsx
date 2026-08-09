import { useParams } from 'react-router-dom'; import { Container, Typography, Chip, Table, TableBody, TableRow, TableCell, Button, Box } from '@mui/material'; import { useQuery, useQueryClient } from '@tanstack/react-query'; import { orderApi } from '../api/orderApi'; import Loading from '../components/shared/Loading'
export default function OrderDetail() { const { id } = useParams(); const qc = useQueryClient(); const { data: order, isLoading } = useQuery({ queryKey: ['order', id], queryFn: () => orderApi.getById(id!) })
  const handleCancel = async () => { try { await orderApi.cancel(id!); qc.invalidateQueries({ queryKey: ['order', id] }) } catch { alert('Cancellation failed') } }
  if (isLoading) return <Loading />
  if (!order) return <Container><Typography>Order not found</Typography></Container>
  return <Container sx={{ mt: 4 }}><Typography variant="h4" mb={3}>Order {order.orderNumber}</Typography><Chip label={order.status} color={order.status === 'CONFIRMED' ? 'success' : 'error'} sx={{ mb: 2 }} />
    <Table><TableBody><TableRow><TableCell>Total</TableCell><TableCell>${order.total}</TableCell></TableRow><TableRow><TableCell>Address</TableCell><TableCell>{order.shippingAddress}</TableCell></TableRow><TableRow><TableCell>Date</TableCell><TableCell>{new Date(order.createdAt).toLocaleString()}</TableCell></TableRow></TableBody></Table>
    <Typography variant="h6" mt={2}>Items</Typography>{order.items?.map((i: any) => <Typography key={i.productId}>{i.productName} x{i.quantity} @ ${i.price}</Typography>)}
    {order.status === 'CONFIRMED' && <Box mt={2}><Button color="error" variant="outlined" onClick={handleCancel}>Cancel Order</Button></Box>}
  </Container>
}
