import { useNavigate, Link as RouterLink } from 'react-router-dom'
import { AppBar, Toolbar, Typography, Button, Badge, IconButton, TextField, Box } from '@mui/material'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'; import FavoriteIcon from '@mui/icons-material/Favorite'
import { useAuth } from '../../hooks/useAuth'; import { useCart } from '../../hooks/useCart'
import { useState } from 'react'

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth(); const { itemCount } = useCart(); const navigate = useNavigate(); const [query, setQuery] = useState('')

  return <AppBar position="sticky"><Toolbar>
    <Typography variant="h6" component={RouterLink} to="/" sx={{ textDecoration: 'none', color: 'white', mr: 4 }}>CommerceSphere</Typography>
    <Box sx={{ flexGrow: 1, display: 'flex', gap: 2 }}>
      <Button color="inherit" onClick={() => navigate('/products')}>Products</Button>
    </Box>
    <TextField size="small" placeholder="Search..." value={query} onChange={e => setQuery(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') navigate(`/search?q=${query}`) }} sx={{ bgcolor: 'white', borderRadius: 1, mr: 2 }} />
    <IconButton color="inherit" onClick={() => navigate('/wishlist')}><FavoriteIcon /></IconButton>
    <IconButton color="inherit" onClick={() => navigate('/cart')}><Badge badgeContent={itemCount} color="error"><ShoppingCartIcon /></Badge></IconButton>
    {isAuthenticated ? (<><Button color="inherit" onClick={() => navigate('/orders')}>Orders</Button><Button color="inherit" onClick={() => navigate('/account')}>{user?.username}</Button><Button color="inherit" onClick={logout}>Logout</Button></>)
      : (<Button color="inherit" onClick={() => navigate('/login')}>Login</Button>)}
  </Toolbar></AppBar>
}
