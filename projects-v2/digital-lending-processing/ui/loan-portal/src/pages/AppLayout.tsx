import { Outlet, useNavigate } from 'react-router-dom'
import { Box, AppBar, Toolbar, Typography, Button, Container, Drawer, List, ListItemButton, ListItemIcon, ListItemText } from '@mui/material'
import { Dashboard, Campaign, Calculate, Description, Notifications, AccountCircle } from '@mui/icons-material'
import { useAuth } from '../contexts/AuthContext'
import { useState } from 'react'

const navItems = [
  { text: 'Dashboard', icon: <Dashboard />, path: '/' },
  { text: 'Loan Offers', icon: <Campaign />, path: '/offers' },
  { text: 'EMI Calculator', icon: <Calculate />, path: '/emi' },
  { text: 'Documents', icon: <Description />, path: '/documents' },
  { text: 'Notifications', icon: <Notifications />, path: '/notifications' },
]

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <Box sx={{ display: 'flex' }}>
      <Drawer variant="permanent" sx={{ width: 240, flexShrink: 0, '& .MuiDrawer-paper': { width: 240 } }}>
        <Box sx={{ p: 2, textAlign: 'center', borderBottom: '1px solid #eee' }}>
          <Typography variant="h6">LoanSphere</Typography>
        </Box>
        <List>
          {navItems.map((item) => (
            <ListItemButton key={item.text} onClick={() => navigate(item.path)}>
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
      <Box sx={{ flexGrow: 1 }}>
        <AppBar position="static">
          <Toolbar>
            <Typography sx={{ flexGrow: 1 }}>Digital Lending Platform</Typography>
            <AccountCircle sx={{ mr: 1 }} />
            <Typography variant="body2" sx={{ mr: 2 }}>{user?.username}</Typography>
            <Button color="inherit" onClick={logout}>Logout</Button>
          </Toolbar>
        </AppBar>
        <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
          <Outlet />
        </Container>
      </Box>
    </Box>
  )
}
