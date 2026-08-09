import { useState } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Button,
  Collapse,
  IconButton,
  Pagination,
  Skeleton,
  Alert,
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import { useOrders, useCancelOrder } from '../hooks/useOrders';
import type { Order, OrderStatus } from '../types';

const STATUS_COLOR: Record<OrderStatus, 'default' | 'primary' | 'info' | 'success' | 'error' | 'warning'> = {
  PENDING: 'warning',
  PROCESSING: 'info',
  SHIPPED: 'primary',
  DELIVERED: 'success',
  CANCELLED: 'error',
};

function OrderRow({ order }: { order: Order }) {
  const [open, setOpen] = useState(false);
  const cancelOrder = useCancelOrder();

  return (
    <>
      <TableRow sx={{ '& > *': { borderBottom: 'unset' } }}>
        <TableCell>
          <IconButton size="small" onClick={() => setOpen(!open)}>
            {open ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
          </IconButton>
        </TableCell>
        <TableCell sx={{ fontWeight: 600 }}>{order.orderNumber}</TableCell>
        <TableCell>{new Date(order.createdAt).toLocaleDateString()}</TableCell>
        <TableCell>
          <Chip
            label={order.status}
            color={STATUS_COLOR[order.status]}
            size="small"
            variant="outlined"
          />
        </TableCell>
        <TableCell>{order.items?.length || 0} items</TableCell>
        <TableCell sx={{ fontWeight: 600 }}>${order.total?.toFixed(2)}</TableCell>
        <TableCell>
          {(order.status === 'PENDING' || order.status === 'PROCESSING') && (
            <Button
              size="small"
              color="error"
              variant="outlined"
              onClick={() => cancelOrder.mutate(order.id)}
              disabled={cancelOrder.isPending}
            >
              Cancel
            </Button>
          )}
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell sx={{ py: 0 }} colSpan={7}>
          <Collapse in={open} timeout="auto" unmountOnExit>
            <Box sx={{ py: 3 }}>
              <Typography variant="subtitle2" gutterBottom>
                Order Items
              </Typography>
              {order.items?.map((item) => (
                <Box
                  key={item.id}
                  sx={{ display: 'flex', gap: 2, alignItems: 'center', py: 1 }}
                >
                  <Box
                    component="img"
                    src={item.image || `https://placehold.co/50x50/1976d2/white?text=X`}
                    alt={item.name}
                    sx={{ width: 50, height: 50, borderRadius: 1, objectFit: 'cover' }}
                  />
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="body2" fontWeight={600}>
                      {item.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Qty: {item.quantity} × ${item.price?.toFixed(2)}
                    </Typography>
                  </Box>
                  <Typography variant="body2" fontWeight={600}>
                    ${(item.price * item.quantity).toFixed(2)}
                  </Typography>
                </Box>
              ))}

              {order.shippingAddress && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Shipping Address
                  </Typography>
                  <Typography variant="body2">
                    {order.shippingAddress.fullName}
                  </Typography>
                  <Typography variant="body2">
                    {order.shippingAddress.street}, {order.shippingAddress.city},{' '}
                    {order.shippingAddress.state} {order.shippingAddress.postalCode}
                  </Typography>
                </Box>
              )}

              {order.trackingInfo && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Tracking
                  </Typography>
                  <Typography variant="body2">
                    {order.trackingInfo.carrier}: {order.trackingInfo.trackingNumber}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Estimated delivery: {order.trackingInfo.estimatedDelivery}
                  </Typography>
                </Box>
              )}
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

export default function OrderHistoryPage() {
  const [page, setPage] = useState(1);
  const { data, isLoading, isError } = useOrders(page, 10);

  const orders = data?.data || [];
  const totalPages = data?.totalPages || 1;

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        My Orders
      </Typography>

      {isLoading ? (
        <Box>
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} variant="rectangular" height={60} sx={{ mb: 1, borderRadius: 1 }} />
          ))}
        </Box>
      ) : isError ? (
        <Alert severity="error">Failed to load orders. Please try again.</Alert>
      ) : orders.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No orders yet
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Start shopping to see your order history here.
          </Typography>
          <Button variant="contained" href="/products">
            Browse Products
          </Button>
        </Box>
      ) : (
        <>
          <TableContainer component={Paper} variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell width={50} />
                  <TableCell>Order #</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Items</TableCell>
                  <TableCell>Total</TableCell>
                  <TableCell width={100} />
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.map((order: Order) => (
                  <OrderRow key={order.id} order={order} />
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={totalPages}
                page={page}
                onChange={(_, p) => setPage(p)}
                color="primary"
              />
            </Box>
          )}
        </>
      )}
    </Box>
  );
}
