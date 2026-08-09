import { Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, CircularProgress } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { authApi } from '../api/authApi'

export default function Customers() {
  const { data: users, isLoading } = useQuery({ queryKey: ['admin-users'], queryFn: () => authApi.listUsers().catch(() => []) })

  return (
    <Box>
      <Typography variant="h4" mb={3}>Customers</Typography>
      {isLoading ? <CircularProgress /> : (
        <TableContainer component={Paper}><Table><TableHead><TableRow><TableCell>Username</TableCell><TableCell>Email</TableCell><TableCell>Name</TableCell><TableCell>Roles</TableCell></TableRow></TableHead>
          <TableBody>{users?.map((u: any) => (
            <TableRow key={u.id}><TableCell>{u.username}</TableCell><TableCell>{u.email}</TableCell><TableCell>{u.firstName} {u.lastName}</TableCell><TableCell>{u.roles?.join(', ')}</TableCell></TableRow>
          ))}</TableBody></Table></TableContainer>
      )}
    </Box>
  )
}
