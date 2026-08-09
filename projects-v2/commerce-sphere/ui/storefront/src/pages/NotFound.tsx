import { Box, Typography, Button } from '@mui/material'; import { useNavigate } from 'react-router-dom'
export default function NotFound() { const n = useNavigate(); return <Box textAlign="center" mt={10}><Typography variant="h2">404</Typography><Typography variant="h5">Page Not Found</Typography><Button variant="contained" onClick={() => n('/')} sx={{ mt: 2 }}>Go Home</Button></Box> }
