import React from 'react'; import ReactDOM from 'react-dom/client'; import App from './App'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'; import { ThemeProvider, CssBaseline } from '@mui/material'
import { SnackbarProvider } from 'notistack'
const qc = new QueryClient({ defaultOptions: { queries: { retry: 1 } } })
import { createTheme } from '@mui/material/styles'
const theme = createTheme({ palette: { primary: { main: '#1976d2' } }, typography: { fontFamily: '"Inter","Roboto","Helvetica","Arial",sans-serif' } })
ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><QueryClientProvider client={qc}><ThemeProvider theme={theme}><CssBaseline /><SnackbarProvider maxSnack={3}><BrowserRouter><App /></BrowserRouter></SnackbarProvider></ThemeProvider></QueryClientProvider></React.StrictMode>)
