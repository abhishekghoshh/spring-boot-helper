import { Box, CircularProgress, Typography } from '@mui/material'
export default function Loading({ message = 'Loading...' }: { message?: string }) {
  return <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" py={8}>
    <CircularProgress /><Typography mt={2} color="text.secondary">{message}</Typography></Box>
}
