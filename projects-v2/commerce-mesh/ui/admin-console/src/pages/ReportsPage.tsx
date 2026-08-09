import { useState } from 'react'
import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Skeleton,
  Alert,
  CircularProgress,
} from '@mui/material'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import PeopleIcon from '@mui/icons-material/People'
import { useQuery } from '@tanstack/react-query'
import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'
import Header from '../components/Header'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface DateRange {
  startDate: string
  endDate: string
}

interface ReportStats {
  totalOrders: number
  totalRevenue: number
  avgOrderValue: number
  newCustomers: number
}

interface RevenueTrendPoint {
  date: string
  revenue: number
}

interface CategoryOrderData {
  name: string
  orders: number
}

interface TopProduct {
  rank: number
  productName: string
  category: string
  unitsSold: number
  revenue: number
}

// ---------------------------------------------------------------------------
// Mock data generators
// ---------------------------------------------------------------------------

const PIE_COLORS = ['#1976d2', '#2e7d32', '#ed6c02', '#7b1fa2', '#c62828', '#00838f', '#6a1b9a', '#4527a0', '#bf360c', '#1b5e20']

function generateRevenueTrend(start: Date, end: Date): RevenueTrendPoint[] {
  const points: RevenueTrendPoint[] = []
  const current = new Date(start)
  while (current <= end) {
    points.push({
      date: current.toISOString().slice(0, 10),
      revenue: Math.round(12000 + Math.random() * 18000),
    })
    current.setDate(current.getDate() + 1)
  }
  return points
}

function generateCategoryData(): CategoryOrderData[] {
  return [
    { name: 'Electronics', orders: 245 },
    { name: 'Clothing', orders: 189 },
    { name: 'Home & Garden', orders: 134 },
    { name: 'Sports', orders: 98 },
    { name: 'Books', orders: 87 },
    { name: 'Toys', orders: 65 },
    { name: 'Food & Beverage', orders: 52 },
    { name: 'Automotive', orders: 41 },
    { name: 'Health', orders: 38 },
    { name: 'Other', orders: 27 },
  ]
}

function generateTopProducts(): TopProduct[] {
  const products = [
    'Wireless Headphones Pro',
    'Organic Cotton T-Shirt',
    'Smart Home Hub',
    'Running Shoes Ultra',
    'Stainless Steel Water Bottle',
    'Bluetooth Speaker',
    'Yoga Mat Premium',
    'Mechanical Keyboard',
    '4K Monitor 27"',
    'Coffee Grinder Electric',
  ]
  const categories = ['Electronics', 'Clothing', 'Electronics', 'Sports', 'Home & Garden', 'Electronics', 'Sports', 'Electronics', 'Electronics', 'Home & Garden']

  return products.map((name, i) => ({
    rank: i + 1,
    productName: name,
    category: categories[i],
    unitsSold: Math.round(50 + Math.random() * 200),
    revenue: Math.round(5000 + Math.random() * 45000),
  }))
}

// ---------------------------------------------------------------------------
// Simulated fetch
// ---------------------------------------------------------------------------

function simulateDelay(ms = 600): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchReportData(dateRange: DateRange): Promise<{
  stats: ReportStats
  revenueTrend: RevenueTrendPoint[]
  categoryData: CategoryOrderData[]
  topProducts: TopProduct[]
}> {
  await simulateDelay(800)
  const start = new Date(dateRange.startDate)
  const end = new Date(dateRange.endDate)

  const totalRevenue = Math.round(45000 + Math.random() * 35000)
  const totalOrders = Math.round(200 + Math.random() * 300)

  return {
    stats: {
      totalOrders,
      totalRevenue,
      avgOrderValue: Math.round(totalRevenue / totalOrders * 100) / 100,
      newCustomers: Math.round(50 + Math.random() * 120),
    },
    revenueTrend: generateRevenueTrend(start, end),
    categoryData: generateCategoryData(),
    topProducts: generateTopProducts(),
  }
}

// ---------------------------------------------------------------------------
// Stat Card sub-component
// ---------------------------------------------------------------------------

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
            color,
          }}
        >
          {icon}
        </Box>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
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

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

export default function ReportsPage() {
  const today = new Date()
  const thirtyDaysAgo = new Date(today)
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)

  const [dateRange, setDateRange] = useState<DateRange>({
    startDate: thirtyDaysAgo.toISOString().slice(0, 10),
    endDate: today.toISOString().slice(0, 10),
  })

  const {
    data,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ['reports', dateRange],
    queryFn: () => fetchReportData(dateRange),
    enabled: !!dateRange.startDate && !!dateRange.endDate,
  })

  // Format helpers
  const fmtCurrency = (value: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)

  const fmtNumber = (value: number) =>
    new Intl.NumberFormat('en-US').format(value)

  const handleDateChange = (field: keyof DateRange) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setDateRange((prev) => ({ ...prev, [field]: e.target.value }))
  }

  // -------------------------------------------------------------------------

  if (isError) {
    return (
      <Box>
        <Header title="Reports" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Reports' }]} />
        <Alert severity="error">
          {(error as Error)?.message || 'Failed to load report data. Please try again.'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Header title="Reports" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Reports' }]} />

      {/* ---- Date Range Picker ---- */}
      <Paper sx={{ p: 2, mb: 3, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
        <TextField
          label="Start Date"
          type="date"
          size="small"
          value={dateRange.startDate}
          onChange={handleDateChange('startDate')}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <Typography variant="body2" color="text.secondary">
          to
        </Typography>
        <TextField
          label="End Date"
          type="date"
          size="small"
          value={dateRange.endDate}
          onChange={handleDateChange('endDate')}
          slotProps={{ inputLabel: { shrink: true } }}
        />
      </Paper>

      {/* ---- Stat Cards ---- */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {isLoading ? (
          ['Total Orders', 'Revenue', 'Avg Order Value', 'New Customers'].map((label) => (
            <Grid size={{ xs: 12, sm: 6, md: 3 }} key={label}>
              <Card sx={{ height: '100%' }}>
                <CardContent sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress size={28} />
                </CardContent>
              </Card>
            </Grid>
          ))
        ) : data ? (
          <>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <StatCard
                label="Total Orders"
                value={fmtNumber(data.stats.totalOrders)}
                icon={<ShoppingCartIcon />}
                color="#1976d2"
                bgColor="#e3f2fd"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <StatCard
                label="Revenue"
                value={fmtCurrency(data.stats.totalRevenue)}
                icon={<AttachMoneyIcon />}
                color="#2e7d32"
                bgColor="#e8f5e9"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <StatCard
                label="Avg Order Value"
                value={fmtCurrency(data.stats.avgOrderValue)}
                icon={<TrendingUpIcon />}
                color="#ed6c02"
                bgColor="#fff3e0"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <StatCard
                label="New Customers"
                value={fmtNumber(data.stats.newCustomers)}
                icon={<PeopleIcon />}
                color="#7b1fa2"
                bgColor="#f3e5f5"
              />
            </Grid>
          </>
        ) : null}
      </Grid>

      {/* ---- Charts Row ---- */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Revenue Trend Line Chart */}
        <Grid size={{ xs: 12, lg: 8 }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Revenue Trend
            </Typography>
            {isLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                <CircularProgress />
              </Box>
            ) : data && data.revenueTrend.length > 0 ? (
              <ResponsiveContainer width="100%" height={350}>
                <LineChart data={data.revenueTrend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="date"
                    tick={{ fontSize: 11 }}
                    tickFormatter={(val: string) => {
                      const d = new Date(val)
                      return `${d.getMonth() + 1}/${d.getDate()}`
                    }}
                  />
                  <YAxis tickFormatter={(val: number) => `$${(val / 1000).toFixed(0)}k`} />
                  <Tooltip
                    formatter={(value) => [fmtCurrency(Number(value)), 'Revenue']}
                    labelFormatter={(label) => new Date(String(label)).toLocaleDateString('en-US', {
                      weekday: 'short',
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  />
                  <Line
                    type="monotone"
                    dataKey="revenue"
                    stroke="#1976d2"
                    strokeWidth={2}
                    dot={false}
                    activeDot={{ r: 4 }}
                    name="Revenue"
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                No revenue trend data for the selected period
              </Typography>
            )}
          </Paper>
        </Grid>

        {/* Orders by Category Pie Chart */}
        <Grid size={{ xs: 12, lg: 4 }}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              Orders by Category
            </Typography>
            {isLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                <CircularProgress />
              </Box>
            ) : data && data.categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height={350}>
                <PieChart>
                  <Pie
                    data={data.categoryData}
                    dataKey="orders"
                    nameKey="name"
                    cx="50%"
                    cy="45%"
                    outerRadius={100}
                    innerRadius={50}
                    paddingAngle={2}
                    label={({ name, percent }) =>
                      `${name} ${((percent ?? 0) * 100).toFixed(0)}%`
                    }
                    labelLine={{ strokeWidth: 1 }}
                  >
                    {data.categoryData.map((_, index) => (
                      <Cell
                        key={index}
                        fill={PIE_COLORS[index % PIE_COLORS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value, name) => [fmtNumber(Number(value)), String(name)]}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                No category data available
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* ---- Top 10 Products Table ---- */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Top 10 Products by Revenue
        </Typography>
        {isLoading ? (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>#</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Product</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Category</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Units Sold</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Revenue</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {Array.from({ length: 10 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton width={20} /></TableCell>
                    <TableCell><Skeleton /></TableCell>
                    <TableCell><Skeleton /></TableCell>
                    <TableCell align="right"><Skeleton /></TableCell>
                    <TableCell align="right"><Skeleton /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : data && data.topProducts.length > 0 ? (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>#</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Product</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Category</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Units Sold</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Revenue</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.topProducts.map((product) => (
                  <TableRow hover key={product.rank}>
                    <TableCell>{product.rank}</TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {product.productName}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {product.category}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">{fmtNumber(product.unitsSold)}</TableCell>
                    <TableCell align="right">{fmtCurrency(product.revenue)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
            No product data available for this period
          </Typography>
        )}
      </Paper>
    </Box>
  )
}
