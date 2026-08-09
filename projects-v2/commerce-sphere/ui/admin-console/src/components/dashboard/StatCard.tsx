import { Card, CardContent, Typography, Box } from '@mui/material'
export default function StatCard({ title, value, icon, color = '#1976d2' }: { title: string; value: string | number; icon: React.ReactNode; color?: string }) {
  return <Card sx={{ height: '100%' }}><CardContent>
    <Box display="flex" justifyContent="space-between" alignItems="center">
      <Box><Typography color="text.secondary" variant="body2">{title}</Typography><Typography variant="h5" fontWeight={600}>{value}</Typography></Box>
      <Box sx={{ color, opacity: 0.7 }}>{icon}</Box>
    </Box></CardContent></Card>
}
