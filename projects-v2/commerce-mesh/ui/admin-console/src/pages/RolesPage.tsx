import { useState, useCallback, useMemo } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  Autocomplete,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Typography,
  Skeleton,
  Alert,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod/v4'
import Header from '../components/Header'
import ConfirmDialog from '../components/ConfirmDialog'
import { useSnackbar } from '../context/SnackbarContext'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface Role {
  id: string
  name: string
  description: string
  permissions: string[]
  createdAt: string
}

interface Permission {
  id: string
  name: string
  description: string
}

// ---------------------------------------------------------------------------
// Mock Data
// ---------------------------------------------------------------------------

const ALL_PERMISSIONS: Permission[] = [
  { id: 'perm-1', name: 'READ_PRODUCTS', description: 'View products' },
  { id: 'perm-2', name: 'WRITE_PRODUCTS', description: 'Create and edit products' },
  { id: 'perm-3', name: 'DELETE_PRODUCTS', description: 'Delete products' },
  { id: 'perm-4', name: 'READ_ORDERS', description: 'View orders' },
  { id: 'perm-5', name: 'WRITE_ORDERS', description: 'Create and edit orders' },
  { id: 'perm-6', name: 'MANAGE_USERS', description: 'Manage user accounts' },
  { id: 'perm-7', name: 'MANAGE_ROLES', description: 'Manage roles and permissions' },
  { id: 'perm-8', name: 'VIEW_REPORTS', description: 'View reports and analytics' },
  { id: 'perm-9', name: 'MANAGE_INVENTORY', description: 'Manage inventory levels' },
  { id: 'perm-10', name: 'MANAGE_CATEGORIES', description: 'Manage product categories' },
  { id: 'perm-11', name: 'VIEW_MONITORING', description: 'View system monitoring' },
  { id: 'perm-12', name: 'MANAGE_SETTINGS', description: 'Manage application settings' },
]

const MOCK_ROLES: Role[] = [
  {
    id: 'role-1',
    name: 'Super Admin',
    description: 'Full access to all features and settings',
    permissions: ALL_PERMISSIONS.map((p) => p.name),
    createdAt: '2025-01-15T10:30:00Z',
  },
  {
    id: 'role-2',
    name: 'Order Manager',
    description: 'Can manage orders and view products',
    permissions: ['READ_PRODUCTS', 'READ_ORDERS', 'WRITE_ORDERS'],
    createdAt: '2025-02-20T14:00:00Z',
  },
  {
    id: 'role-3',
    name: 'Product Manager',
    description: 'Can manage products and categories',
    permissions: ['READ_PRODUCTS', 'WRITE_PRODUCTS', 'DELETE_PRODUCTS', 'MANAGE_CATEGORIES', 'MANAGE_INVENTORY'],
    createdAt: '2025-03-10T09:15:00Z',
  },
  {
    id: 'role-4',
    name: 'Viewer',
    description: 'Read-only access to dashboards and reports',
    permissions: ['READ_PRODUCTS', 'READ_ORDERS', 'VIEW_REPORTS'],
    createdAt: '2025-04-05T16:45:00Z',
  },
  {
    id: 'role-5',
    name: 'Support Agent',
    description: 'Can view orders and customers',
    permissions: ['READ_ORDERS', 'READ_PRODUCTS'],
    createdAt: '2025-05-12T11:20:00Z',
  },
]

// ---------------------------------------------------------------------------
// Simulated API helpers
// ---------------------------------------------------------------------------

let rolesStore = [...MOCK_ROLES]

function simulateDelay(ms = 500): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchRoles(): Promise<Role[]> {
  await simulateDelay(400)
  return [...rolesStore]
}

async function createRole(data: { name: string; description: string; permissions: string[] }): Promise<Role> {
  await simulateDelay(600)
  const newRole: Role = {
    id: `role-${Date.now()}`,
    name: data.name,
    description: data.description,
    permissions: data.permissions,
    createdAt: new Date().toISOString(),
  }
  rolesStore = [...rolesStore, newRole]
  return newRole
}

async function updateRole(id: string, data: { name: string; description: string; permissions: string[] }): Promise<Role> {
  await simulateDelay(600)
  const index = rolesStore.findIndex((r) => r.id === id)
  if (index === -1) throw new Error('Role not found')
  const updated: Role = {
    ...rolesStore[index],
    name: data.name,
    description: data.description,
    permissions: data.permissions,
  }
  rolesStore = [...rolesStore.slice(0, index), updated, ...rolesStore.slice(index + 1)]
  return updated
}

async function deleteRole(id: string): Promise<void> {
  await simulateDelay(400)
  rolesStore = rolesStore.filter((r) => r.id !== id)
}

// ---------------------------------------------------------------------------
// Form Schema
// ---------------------------------------------------------------------------

const roleFormSchema = z.object({
  name: z.string().min(1, 'Role name is required').max(100, 'Role name is too long'),
  description: z.string().min(1, 'Description is required').max(500, 'Description is too long'),
  permissions: z.array(z.string()).min(1, 'At least one permission is required'),
})

type RoleFormData = z.infer<typeof roleFormSchema>

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function RolesPage() {
  const queryClient = useQueryClient()
  const { showSnackbar } = useSnackbar()

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<Role | null>(null)

  // Delete confirmation state
  const [deleteTarget, setDeleteTarget] = useState<Role | null>(null)

  // ---- Queries ----

  const {
    data: roles,
    isLoading,
    isError,
    error,
  } = useQuery<Role[]>({
    queryKey: ['roles'],
    queryFn: fetchRoles,
  })

  // ---- Mutations ----

  const saveMutation = useMutation({
    mutationFn: (data: RoleFormData) =>
      editingRole ? updateRole(editingRole.id, data) : createRole(data),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      showSnackbar(
        editingRole
          ? `Role "${result.name}" updated successfully`
          : `Role "${result.name}" created successfully`,
        'success'
      )
      handleCloseDialog()
    },
    onError: (err: Error) => {
      showSnackbar(err.message || 'Failed to save role', 'error')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteRole(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] })
      showSnackbar('Role deleted successfully', 'success')
      setDeleteTarget(null)
    },
    onError: (err: Error) => {
      showSnackbar(err.message || 'Failed to delete role', 'error')
      setDeleteTarget(null)
    },
  })

  // ---- Form ----

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<RoleFormData>({
    resolver: zodResolver(roleFormSchema),
    defaultValues: {
      name: '',
      description: '',
      permissions: [],
    },
  })

  // ---- Dialog handlers ----

  const handleOpenCreateDialog = useCallback(() => {
    setEditingRole(null)
    reset({ name: '', description: '', permissions: [] })
    setDialogOpen(true)
  }, [reset])

  const handleOpenEditDialog = useCallback(
    (role: Role) => {
      setEditingRole(role)
      reset({
        name: role.name,
        description: role.description,
        permissions: role.permissions,
      })
      setDialogOpen(true)
    },
    [reset]
  )

  const handleCloseDialog = useCallback(() => {
    setDialogOpen(false)
    setEditingRole(null)
  }, [])

  const onSubmit = useCallback(
    (data: RoleFormData) => {
      saveMutation.mutate(data)
    },
    [saveMutation]
  )

  // ---- Delete handlers ----

  const handleDeleteClick = useCallback((role: Role) => {
    setDeleteTarget(role)
  }, [])

  const handleDeleteConfirm = useCallback(() => {
    if (deleteTarget) {
      deleteMutation.mutate(deleteTarget.id)
    }
  }, [deleteTarget, deleteMutation])

  const handleDeleteCancel = useCallback(() => {
    setDeleteTarget(null)
  }, [])

  // ---- Derived: permission options for Autocomplete ----

  const permissionOptions = useMemo(
    () => ALL_PERMISSIONS.map((p) => p.name),
    []
  )

  // -------------------------------------------------------------------------
  // Render helpers
  // -------------------------------------------------------------------------

  function renderContent() {
    if (isLoading) {
      return (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Permissions</TableCell>
                <TableCell>Created At</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell><Skeleton /></TableCell>
                  <TableCell><Skeleton /></TableCell>
                  <TableCell><Skeleton width="80%" /></TableCell>
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
          {(error as Error)?.message || 'Failed to load roles. Please try again.'}
        </Alert>
      )
    }

    if (!roles || roles.length === 0) {
      return (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">No roles found</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Click &quot;Add Role&quot; to create the first role.
          </Typography>
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
              <TableCell sx={{ fontWeight: 600 }}>Permissions</TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Created At</TableCell>
              <TableCell align="right" sx={{ fontWeight: 600 }}>
                Actions
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {roles.map((role) => (
              <TableRow hover key={role.id}>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>
                    {role.name}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {role.description}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {role.permissions.length === 0 ? (
                      <Typography variant="body2" color="text.secondary">
                        None
                      </Typography>
                    ) : (
                      role.permissions.map((perm) => (
                        <Chip key={perm} label={perm} size="small" variant="outlined" />
                      ))
                    )}
                  </Box>
                </TableCell>
                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {new Date(role.createdAt).toLocaleDateString('en-US', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
                    <Button
                      size="small"
                      variant="outlined"
                      startIcon={<EditIcon fontSize="small" />}
                      onClick={() => handleOpenEditDialog(role)}
                    >
                      Edit
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      color="error"
                      startIcon={<DeleteIcon fontSize="small" />}
                      onClick={() => handleDeleteClick(role)}
                    >
                      Delete
                    </Button>
                  </Box>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    )
  }

  // -------------------------------------------------------------------------

  return (
    <Box>
      <Header
        title="Roles"
        breadcrumbs={[
          { label: 'Admin', path: '/' },
          { label: 'Roles' },
        ]}
      />

      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleOpenCreateDialog}
        >
          Add Role
        </Button>
      </Box>

      {renderContent()}

      {/* ---- Create / Edit Dialog ---- */}
      <Dialog
        open={dialogOpen}
        onClose={handleCloseDialog}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <DialogTitle>
            {editingRole ? `Edit Role: ${editingRole.name}` : 'Create New Role'}
          </DialogTitle>
          <DialogContent dividers>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, pt: 1 }}>
              <TextField
                label="Role Name"
                fullWidth
                {...register('name')}
                error={!!errors.name}
                helperText={errors.name?.message}
                autoFocus
              />
              <TextField
                label="Description"
                fullWidth
                multiline
                rows={3}
                {...register('description')}
                error={!!errors.description}
                helperText={errors.description?.message}
              />
              <Controller
                name="permissions"
                control={control}
                render={({ field }) => (
                  <Autocomplete
                    multiple
                    options={permissionOptions}
                    value={field.value}
                    onChange={(_, newValue) => field.onChange(newValue)}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Permissions"
                        error={!!errors.permissions}
                        helperText={errors.permissions?.message}
                      />
                    )}
                  />
                )}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseDialog} disabled={saveMutation.isPending}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={saveMutation.isPending || isSubmitting}
            >
              {saveMutation.isPending ? (
                <CircularProgress size={20} color="inherit" />
              ) : editingRole ? (
                'Update'
              ) : (
                'Create'
              )}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* ---- Delete Confirmation Dialog ---- */}
      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Role"
        message={
          deleteTarget
            ? `Are you sure you want to delete the role "${deleteTarget.name}"? This action cannot be undone.`
            : ''
        }
        confirmLabel="Delete"
        confirmColor="error"
        loading={deleteMutation.isPending}
        onConfirm={handleDeleteConfirm}
        onCancel={handleDeleteCancel}
      />
    </Box>
  )
}
