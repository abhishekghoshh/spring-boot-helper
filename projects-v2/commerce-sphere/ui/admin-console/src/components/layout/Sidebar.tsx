import { useNavigate, useLocation } from 'react-router-dom'
import { Drawer, List, ListItemButton, ListItemIcon, ListItemText, Toolbar, Divider } from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import InventoryIcon from '@mui/icons-material/Inventory'
import CategoryIcon from '@mui/icons-material/Category'
import BrandingWatermarkIcon from '@mui/icons-material/BrandingWatermark'
import WarehouseIcon from '@mui/icons-material/Warehouse'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import PeopleIcon from '@mui/icons-material/People'
import SettingsIcon from '@mui/icons-material/Settings'
import LogoutIcon from '@mui/icons-material/Logout'
import { useAuth } from '../../hooks/useAuth'

const items = [
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
  { text: 'Products', icon: <InventoryIcon />, path: '/products' },
  { text: 'Categories', icon: <CategoryIcon />, path: '/categories' },
  { text: 'Brands', icon: <BrandingWatermarkIcon />, path: '/brands' },
  { text: 'Inventory', icon: <WarehouseIcon />, path: '/inventory' },
  { text: 'Orders', icon: <ShoppingCartIcon />, path: '/orders' },
  { text: 'Customers', icon: <PeopleIcon />, path: '/customers' },
  { text: 'Settings', icon: <SettingsIcon />, path: '/settings' },
]

export default function Sidebar({ open }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useAuth()
  return (
    <Drawer variant="persistent" open={open} sx={{ width: 240, '& .MuiDrawer-paper': { width: 240 } }}>
      <Toolbar />
      <List>
        {items.map((item) => (
          <ListItemButton key={item.path} selected={location.pathname === item.path} onClick={() => navigate(item.path)}>
            <ListItemIcon>{item.icon}</ListItemIcon>
            <ListItemText primary={item.text} />
          </ListItemButton>
        ))}
      </List>
      <Divider />
      <List>
        <ListItemButton onClick={logout}>
          <ListItemIcon><LogoutIcon /></ListItemIcon>
          <ListItemText primary="Logout" />
        </ListItemButton>
      </List>
    </Drawer>
  )
}
