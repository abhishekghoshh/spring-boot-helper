import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  Badge,
  IconButton,
  Button,
  Menu,
  MenuItem,
  Divider,
  Avatar,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import PersonOutlinedIcon from '@mui/icons-material/PersonOutlined';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import LogoutIcon from '@mui/icons-material/Logout';
import SettingsIcon from '@mui/icons-material/Settings';
import { useAuth } from '../hooks/useAuth';
import { useCart } from '../hooks/useCart';
import SearchBar from './SearchBar';

const CATEGORIES = ['Electronics', 'Fashion', 'Home & Garden', 'Sports', 'Books', 'Toys'];

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const { itemCount } = useCart();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  const handleMenuOpen = (e: React.MouseEvent<HTMLElement>) => setAnchorEl(e.currentTarget);
  const handleMenuClose = () => setAnchorEl(null);

  const handleNavigate = (path: string) => {
    handleMenuClose();
    navigate(path);
  };

  const handleLogout = () => {
    handleMenuClose();
    logout();
    navigate('/');
  };

  return (
    <AppBar position="sticky" color="inherit" elevation={1} sx={{ bgcolor: 'white' }}>
      <Toolbar sx={{ gap: 2, minHeight: { xs: 56, sm: 64 } }}>
        {/* Logo */}
        <Typography
          variant="h5"
          component={Link}
          to="/"
          sx={{
            textDecoration: 'none',
            color: 'primary.main',
            fontWeight: 800,
            letterSpacing: -1,
            flexShrink: 0,
            fontSize: { xs: '1.2rem', sm: '1.5rem' },
          }}
        >
          CommerceMesh
        </Typography>

        {/* Search */}
        <Box sx={{ flex: 1, maxWidth: 560, mx: { xs: 1, sm: 3 } }}>
          <SearchBar />
        </Box>

        {/* Actions */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexShrink: 0 }}>
          {isAuthenticated ? (
            <>
              <IconButton onClick={() => navigate('/wishlist')} aria-label="wishlist">
                <FavoriteBorderIcon />
              </IconButton>
              <IconButton onClick={() => navigate('/cart')} aria-label="cart">
                <Badge badgeContent={itemCount} color="primary">
                  <ShoppingCartIcon />
                </Badge>
              </IconButton>
              <Button
                onClick={handleMenuOpen}
                color="inherit"
                size="small"
                sx={{ textTransform: 'none', ml: 0.5, display: { xs: 'none', sm: 'inline-flex' } }}
                startIcon={
                  <Avatar sx={{ width: 28, height: 28, bgcolor: 'primary.main', fontSize: 14 }}>
                    {user?.fullName?.charAt(0) || user?.username?.charAt(0) || 'U'}
                  </Avatar>
                }
              >
                {user?.fullName || user?.username}
              </Button>
              <IconButton onClick={handleMenuOpen} sx={{ display: { xs: 'inline-flex', sm: 'none' } }}>
                <PersonOutlinedIcon />
              </IconButton>
              <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={handleMenuClose}
                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                PaperProps={{ sx: { minWidth: 200, mt: 1 } }}
              >
                <MenuItem onClick={() => handleNavigate('/profile')}>
                  <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
                  <ListItemText>My Profile</ListItemText>
                </MenuItem>
                <MenuItem onClick={() => handleNavigate('/orders')}>
                  <ListItemIcon><ReceiptLongIcon fontSize="small" /></ListItemIcon>
                  <ListItemText>My Orders</ListItemText>
                </MenuItem>
                <MenuItem onClick={() => handleNavigate('/wishlist')}>
                  <ListItemIcon><FavoriteBorderIcon fontSize="small" /></ListItemIcon>
                  <ListItemText>Wishlist</ListItemText>
                </MenuItem>
                <Divider />
                <MenuItem onClick={handleLogout}>
                  <ListItemIcon><LogoutIcon fontSize="small" /></ListItemIcon>
                  <ListItemText>Logout</ListItemText>
                </MenuItem>
              </Menu>
            </>
          ) : (
            <>
              <Button
                component={Link}
                to="/login"
                color="inherit"
                size="small"
                sx={{ textTransform: 'none' }}
              >
                Login
              </Button>
              <Button
                component={Link}
                to="/register"
                variant="contained"
                size="small"
                sx={{ textTransform: 'none', ml: 1 }}
              >
                Sign Up
              </Button>
            </>
          )}
        </Box>
      </Toolbar>

      {/* Category bar */}
      <Toolbar
        variant="dense"
        sx={{
          borderTop: 1,
          borderColor: 'divider',
          gap: 1,
          justifyContent: 'center',
          overflowX: 'auto',
          display: { xs: 'none', sm: 'flex' },
        }}
      >
        {CATEGORIES.map((cat) => (
          <Button
            key={cat}
            component={Link}
            to={`/products?category=${encodeURIComponent(cat)}`}
            color="inherit"
            size="small"
            sx={{ textTransform: 'none', whiteSpace: 'nowrap', minWidth: 'auto' }}
          >
            {cat}
          </Button>
        ))}
      </Toolbar>
    </AppBar>
  );
}
