import { Box, Typography, Grid, Card, CardContent, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from '@mui/material'
import { People, Assignment, Campaign, TrendingUp, CheckCircle, Cancel } from '@mui/icons-material'

const stats = [
  { label: 'Total Customers', value: '—', icon: <People />, color: '#1976d2' },
  { label: 'Pending Applications', value: '—', icon: <Assignment />, color: '#f57c00' },
  { label: 'Active Offers', value: '—', icon: <Campaign />, color: '#388e3c' },
  { label: 'Approved Loans', value: '—', icon: <CheckCircle />, color: '#1976d2' },
  { label: 'Rejected Loans', value: '—', icon: <Cancel />, color: '#d32f2f' },
  { label: "Today's Applications", value: '—', icon: <TrendingUp />, color: '#7b1fa2' },
]

export default function DashboardPage() {
  return (
    <Box>
      <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>Admin Dashboard</Typography>

      <Grid container spacing={3} mb={4}>
        {stats.map((stat, i) => (
          <Grid item xs={12} sm={6} md={4} lg={2} key={i}>
            <Card sx={{ borderTop: `3px solid ${stat.color}`, borderRadius: 2 }}>
              <CardContent>
                <Box display="flex" alignItems="center" gap={1} mb={1}>
                  <Box sx={{ color: stat.color }}>{stat.icon}</Box>
                </Box>
                <Typography variant="h4" sx={{ fontWeight: 700 }}>{stat.value}</Typography>
                <Typography variant="body2" color="text.secondary">{stat.label}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Typography variant="h6" gutterBottom sx={{ fontWeight: 600 }}>Recent Applications</Typography>
      <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: '#f5f5f5' }}>
              <TableCell><strong>Application ID</strong></TableCell>
              <TableCell><strong>Customer</strong></TableCell>
              <TableCell><strong>Loan Offer</strong></TableCell>
              <TableCell><strong>Amount</strong></TableCell>
              <TableCell><strong>Status</strong></TableCell>
              <TableCell><strong>Submitted</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                Connect to backend to load application data
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}
