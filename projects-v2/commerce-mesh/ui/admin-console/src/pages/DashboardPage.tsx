import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  CircularProgress,
  Alert,
} from '@mui/material'
import PeopleIcon from '@mui/icons-material/People'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import InventoryIcon from '@mui/icons-material/Inventory'
import WarningIcon from '@mui/icons-material/Warning'
import ErrorIcon from '@mui/icons-material/Error'
import { useQuery } from '@tanstack/react-query'
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'
import { getDashboardStats, getOrderTrends, getTopProducts } from '../api/dashboardApi'

interface StatCardProps {
  label: string
  value: string | number
  icon: React.ReactNode
  color: string
  bgColor: string
}

function StatCard({ label, value, icon, color, bgColor }: StatCardProps) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 56,
            height: 56,
            borderRadius: 2,
            bgcolor: bgColor,
            color: color,
          }}
        >
          {icon}
        </Box>
        <Box>
          <Typography variant="h4" fontWeight={700}>
            {value}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {label}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const {
    data: stats,
    isLoading: statsLoading,
    error: statsError,
  } = useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: getDashboardStats,
  })

  const {
    data: orderTrends,
    isLoading: trendsLoading,
  } = useQuery({
    queryKey: ['dashboard', 'order-trends'],
    queryFn: getOrderTrends,
  })

  const {
    data: topProducts,
    isLoading: topProductsLoading,
  } = useQuery({
    queryKey: ['dashboard', 'top-products'],
    queryFn: getTopProducts,
  })

  const statCards = [
    {
      label: 'Active Users',
      value: stats?.activeUsers ?? '-',
      icon: <PeopleIcon />,
      color: '#1976d2',
      bgColor: '#e3f2fd',
    },
    {
      label: 'Orders Today',
      value: stats?.ordersToday ?? '-',
      icon: <ShoppingCartIcon />,
      color: '#ed6c02',
      bgColor: '#fff3e0',
    },
    {
      label: 'Revenue',
      value: stats?.revenue != null
        ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(stats.revenue)
        : '-',
      icon: <AttachMoneyIcon />,
      color: '#2e7d32',
      bgColor: '#e8f5e9',
    },
    {
      label: 'Products Count',
      value: stats?.productsCount ?? '-',
      icon: <InventoryIcon />,
      color: '#7b1fa2',
      bgColor: '#f3e5f5',
    },
    {
      label: 'Low Stock Items',
      value: stats?.lowStockCount ?? '-',
      icon: <WarningIcon />,
      color: '#d32f2f',
      bgColor: '#ffebee',
    },
    {
      label: 'Failed Payments',
      value: stats?.failedPayments ?? '-',
      icon: <ErrorIcon />,
      color: '#c62828',
      bgColor: '#ffcdd2',
    },
  ]

  if (statsError) {
    return (
      <Box>
        <Typography variant="h4" gutterBottom>
          Dashboard
        </Typography>
        <Alert severity="error">Failed to load dashboard stats. Please try again later.</Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom sx={{ mb: 3 }}>
        Dashboard
      </Typography>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        {statsLoading
          ? statCards.map((stat) => (
              <Grid item xs={12} sm={6} md={4} key={stat.label}>
                <Card sx={{ height: '100%' }}>
                  <CardContent
                    sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4 }}
                  >
                    <CircularProgress size={28} />
                  </CardContent>
                </Card>
              </Grid>
            ))
          : statCards.map((stat) => (
              <Grid item xs={12} sm={6} md={4} key={stat.label}>
                <StatCard {...stat} />
              </Grid>
            ))}
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} lg={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Orders & Revenue Trend (Last 7 Days)
            </Typography>
            {trendsLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                <CircularProgress />
              </Box>
            ) : orderTrends && orderTrends.length > 0 ? (
              <ResponsiveContainer width="100%" height={350}>
                <LineChart data={orderTrends}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis yAxisId="left" />
                  <YAxis yAxisId="right" orientation="right" />
                  <Tooltip />
                  <Line
                    yAxisId="left"
                    type="monotone"
                    dataKey="orders"
                    stroke="#1976d2"
                    strokeWidth={2}
                    name="Orders"
                  />
                  <Line
                    yAxisId="right"
                    type="monotone"
                    dataKey="revenue"
                    stroke="#2e7d32"
                    strokeWidth={2}
                    name="Revenue ($)"
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                No trend data available
              </Typography>
            )}
          </Paper>
        </Grid>

        <Grid item xs={12} lg={4}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              Top Products by Revenue
            </Typography>
            {topProductsLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                <CircularProgress />
              </Box>
            ) : topProducts && topProducts.length > 0 ? (
              <ResponsiveContainer width="100%" height={350}>
                <BarChart
                  data={topProducts}
                  layout="vertical"
                  margin={{ left: 20, right: 20 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" />
                  <YAxis
                    type="category"
                    dataKey="productName"
                    width={100}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip />
                  <Bar dataKey="revenue" fill="#7b1fa2" name="Revenue ($)" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                No product data available
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
