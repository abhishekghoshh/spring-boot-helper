import { Box, Paper, Typography, Grid, Card, CardContent } from '@mui/material'
import { useAuth } from '../contexts/AuthContext'

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <Box>
      <Typography variant="h4" gutterBottom>Welcome, {user?.firstName || user?.username}</Typography>
      <Grid container spacing={3}>
        {[
          { title: 'Active Loan Offers', value: '--', color: '#1976d2' },
          { title: 'My Applications', value: '--', color: '#388e3c' },
          { title: 'Approved Loans', value: '--', color: '#f57c00' },
          { title: 'Pending Review', value: '--', color: '#d32f2f' },
        ].map((card, i) => (
          <Grid item xs={12} sm={6} md={3} key={i}>
            <Card sx={{ borderLeft: `4px solid ${card.color}` }}>
              <CardContent>
                <Typography color="text.secondary" variant="body2">{card.title}</Typography>
                <Typography variant="h4">{card.value}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}
