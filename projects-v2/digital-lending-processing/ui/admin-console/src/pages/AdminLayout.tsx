import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Box, AppBar, Toolbar, Typography, Button, Drawer, List, ListItemButton, ListItemIcon, ListItemText, Chip } from '@mui/material'
import {
  Dashboard, People, Campaign, Assignment, TaskAlt, Settings, AccountCircle,
} from '@mui/icons-material'
import { useAuth } from '../contexts/AuthContext'

const navItems = [
  { text: 'Dashboard', icon: <Dashboard />, path: '/' },
  { text: 'Users', icon: <People />, path: '/users' },
  { text: 'Loan Offers', icon: <Campaign />, path: '/offers' },
  { text: 'Applications', icon: <Assignment />, path: '/applications' },
  { text: 'Approvals', icon: <TaskAlt />, path: '/approvals' },
  { text: 'Settings', icon: <Settings />, path: '/settings' },
]

export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <Box sx={{ display: 'flex', bgcolor: '#f5f7fa', minHeight: '100vh' }}>
      <Drawer variant="permanent" sx={{
        width: 260, flexShrink: 0,
        '& .MuiDrawer-paper': { width: 260, bgcolor: '#0a1929', color: '#fff', borderRight: 'none' }
      }}>
        <Box sx={{ p: 3, borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
          <Typography variant="h5" sx={{ fontWeight: 700, color: '#42a5f5' }}>LoanSphere</Typography>
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)' }}>Admin Console</Typography>
        </Box>
        <List sx={{ px: 1, mt: 1 }}>
          {navItems.map((item) => (
            <ListItemButton
              key={item.text}
              onClick={() => navigate(item.path)}
              sx={{
                borderRadius: 1, mb: 0.5, color: 'rgba(255,255,255,0.7)',
                '&:hover': { bgcolor: 'rgba(255,255,255,0.08)', color: '#fff' },
                ...(location.pathname === item.path && { bgcolor: 'rgba(66,165,245,0.15)', color: '#42a5f5' })
              }}
            >
              <ListItemIcon sx={{ minWidth: 40, color: 'inherit' }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>

      <Box sx={{ flexGrow: 1 }}>
        <AppBar position="static" elevation={0} sx={{ bgcolor: '#fff', color: '#333', borderBottom: '1px solid #e0e0e0' }}>
          <Toolbar>
            <Typography variant="body2" sx={{ flexGrow: 1, color: '#666' }}>
              {navItems.find(i => i.path === location.pathname)?.text || 'Dashboard'}
            </Typography>
            <Chip
              avatar={<AccountCircle />}
              label={`${user?.username} — ${user?.roles?.join(', ')}`}
              variant="outlined" size="small" sx={{ mr: 2 }}
            />
            <Button
              variant="outlined" color="error" size="small"
              onClick={logout}
            >
              Logout
            </Button>
          </Toolbar>
        </AppBar>
        <Box sx={{ p: 4 }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}
