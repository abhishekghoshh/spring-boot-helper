import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Skeleton,
  Alert,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import Header from '../components/Header'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface Permission {
  id: string
  name: string
  description: string
}

// ---------------------------------------------------------------------------
// Mock Data
// ---------------------------------------------------------------------------

const PERMISSIONS: Permission[] = [
  { id: 'perm-1', name: 'READ_PRODUCTS', description: 'View products in the catalog' },
  { id: 'perm-2', name: 'WRITE_PRODUCTS', description: 'Create and edit product details' },
  { id: 'perm-3', name: 'DELETE_PRODUCTS', description: 'Permanently delete products' },
  { id: 'perm-4', name: 'READ_ORDERS', description: 'View orders and order details' },
  { id: 'perm-5', name: 'WRITE_ORDERS', description: 'Create and update orders' },
  { id: 'perm-6', name: 'MANAGE_USERS', description: 'Manage user accounts and roles' },
  { id: 'perm-7', name: 'MANAGE_ROLES', description: 'Create, update, and delete roles' },
  { id: 'perm-8', name: 'VIEW_REPORTS', description: 'Access reports and analytics' },
  { id: 'perm-9', name: 'MANAGE_INVENTORY', description: 'Manage inventory levels and warehouses' },
  { id: 'perm-10', name: 'MANAGE_CATEGORIES', description: 'Manage product categories' },
  { id: 'perm-11', name: 'VIEW_MONITORING', description: 'View system monitoring dashboards' },
  { id: 'perm-12', name: 'MANAGE_SETTINGS', description: 'Modify application settings' },
]

// ---------------------------------------------------------------------------
// Simulated fetch
// ---------------------------------------------------------------------------

function simulateDelay(ms = 400): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchPermissions(): Promise<Permission[]> {
  await simulateDelay(300)
  return [...PERMISSIONS]
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function PermissionsPage() {
  const { data: permissions, isLoading, isError, error } = useQuery<Permission[]>({
    queryKey: ['permissions'],
    queryFn: fetchPermissions,
  })

  function renderContent() {
    if (isLoading) {
      return (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 600 }}>Name</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Description</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {Array.from({ length: 8 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell><Skeleton /></TableCell>
                  <TableCell><Skeleton /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )
    }

    if (isError) {
      return (
        <Alert severity="error">
          {(error as Error)?.message || 'Failed to load permissions. Please try again.'}
        </Alert>
      )
    }

    if (!permissions || permissions.length === 0) {
      return (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">No permissions found</Typography>
        </Paper>
      )
    }

    return (
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 600 }}>Name</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Description</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {permissions.map((perm) => (
              <TableRow hover key={perm.id}>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 500, fontFamily: 'monospace' }}>
                    {perm.name}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {perm.description}
                  </Typography>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    )
  }

  return (
    <Box>
      <Header
        title="Permissions"
        breadcrumbs={[
          { label: 'Admin', path: '/' },
          { label: 'Permissions' },
        ]}
      />

      <Paper sx={{ p: 2, mb: 2, bgcolor: 'info.light', color: 'info.contrastText' }}>
        <Typography variant="body2">
          Permissions are seeded and managed at the application level. They cannot be created or
          deleted from this interface. Use the Roles page to assign permissions to roles.
        </Typography>
      </Paper>

      {renderContent()}
    </Box>
  )
}
