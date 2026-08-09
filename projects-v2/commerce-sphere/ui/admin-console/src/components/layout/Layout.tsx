import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Box, AppBar, Toolbar, Typography, IconButton, Drawer as MuiDrawer } from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import Sidebar from './Sidebar'
import { useAuth } from '../../hooks/useAuth'

export default function Layout() {
  const [open, setOpen] = useState(true)
  const { user, logout } = useAuth()
  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <IconButton color="inherit" edge="start" onClick={() => setOpen(!open)} sx={{ mr: 2 }}><MenuIcon /></IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>CommerceSphere Admin</Typography>
          <Typography variant="body2" sx={{ mr: 2 }}>{user?.username}</Typography>
        </Toolbar>
      </AppBar>
      <Sidebar open={open} onClose={() => setOpen(false)} />
      <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8, ml: open ? '240px' : 0, transition: 'margin 0.3s' }}>
        <Outlet />
      </Box>
    </Box>
  )
}
