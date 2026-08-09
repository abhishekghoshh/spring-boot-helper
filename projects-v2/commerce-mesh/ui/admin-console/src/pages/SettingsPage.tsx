import { useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Switch,
  FormControlLabel,
  Divider,
  CircularProgress,
  Alert,
  Skeleton,
} from '@mui/material'
import SaveIcon from '@mui/icons-material/Save'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod/v4'
import Header from '../components/Header'
import { useSnackbar } from '../context/SnackbarContext'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface AppSettings {
  general: {
    siteName: string
    supportEmail: string
    itemsPerPage: number
  }
  security: {
    sessionTimeoutMinutes: number
    maxLoginAttempts: number
  }
  notifications: {
    emailNotifications: boolean
    smsNotifications: boolean
  }
}

// ---------------------------------------------------------------------------
// Mock data
// ---------------------------------------------------------------------------

const MOCK_SETTINGS: AppSettings = {
  general: {
    siteName: 'CommerceMesh',
    supportEmail: 'support@commercemesh.com',
    itemsPerPage: 25,
  },
  security: {
    sessionTimeoutMinutes: 30,
    maxLoginAttempts: 5,
  },
  notifications: {
    emailNotifications: true,
    smsNotifications: false,
  },
}

// ---------------------------------------------------------------------------
// Simulated API
// ---------------------------------------------------------------------------

function simulateDelay(ms = 500): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

let currentSettings = { ...MOCK_SETTINGS }

async function fetchSettings(): Promise<AppSettings> {
  await simulateDelay(400)
  return { ...currentSettings }
}

async function saveSettings(data: AppSettings): Promise<AppSettings> {
  await simulateDelay(800)
  currentSettings = { ...data, general: { ...data.general }, security: { ...data.security }, notifications: { ...data.notifications } }
  return { ...currentSettings }
}

// ---------------------------------------------------------------------------
// Zod Schema
// ---------------------------------------------------------------------------

const settingsSchema = z.object({
  general: z.object({
    siteName: z.string().min(1, 'Site name is required').max(200, 'Site name is too long'),
    supportEmail: z.string().min(1, 'Support email is required').email('Must be a valid email'),
    itemsPerPage: z.number().int().min(5, 'Minimum 5').max(100, 'Maximum 100'),
  }),
  security: z.object({
    sessionTimeoutMinutes: z.number().int().min(5, 'Minimum 5 minutes').max(1440, 'Maximum 1440 minutes (24 hours)'),
    maxLoginAttempts: z.number().int().min(1, 'Minimum 1').max(20, 'Maximum 20'),
  }),
  notifications: z.object({
    emailNotifications: z.boolean(),
    smsNotifications: z.boolean(),
  }),
})

type SettingsFormData = z.infer<typeof settingsSchema>

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function SettingsPage() {
  const queryClient = useQueryClient()
  const { showSnackbar } = useSnackbar()

  // ---- Fetch current settings ----
  const {
    data: settings,
    isLoading,
    isError,
    error,
  } = useQuery<AppSettings>({
    queryKey: ['settings'],
    queryFn: fetchSettings,
  })

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isDirty },
  } = useForm<SettingsFormData>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      general: { siteName: '', supportEmail: '', itemsPerPage: 25 },
      security: { sessionTimeoutMinutes: 30, maxLoginAttempts: 5 },
      notifications: { emailNotifications: true, smsNotifications: false },
    },
  })

  // Reset form when settings are loaded
  useEffect(() => {
    if (settings) {
      reset({
        general: { ...settings.general },
        security: { ...settings.security },
        notifications: { ...settings.notifications },
      })
    }
  }, [settings, reset])

  // ---- Save mutation ----
  const saveMutation = useMutation({
    mutationFn: (data: SettingsFormData) => saveSettings(data as AppSettings),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settings'] })
      showSnackbar('Settings saved successfully', 'success')
    },
    onError: (err: Error) => {
      showSnackbar(err.message || 'Failed to save settings', 'error')
    },
  })

  const onSubmit = (data: SettingsFormData) => {
    saveMutation.mutate(data)
  }

  // -------------------------------------------------------------------------

  if (isError) {
    return (
      <Box>
        <Header
          title="Settings"
          breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Settings' }]}
        />
        <Alert severity="error">
          {(error as Error)?.message || 'Failed to load settings. Please try again.'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Header
        title="Settings"
        breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Settings' }]}
      />

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {/* ---- General Section ---- */}
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            General
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Basic application configuration
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, maxWidth: 600 }}>
            {isLoading ? (
              <>
                <Skeleton variant="rectangular" height={56} />
                <Skeleton variant="rectangular" height={56} />
                <Skeleton variant="rectangular" height={56} />
              </>
            ) : (
              <>
                <TextField
                  label="Site Name"
                  fullWidth
                  {...register('general.siteName')}
                  error={!!errors.general?.siteName}
                  helperText={errors.general?.siteName?.message}
                />
                <TextField
                  label="Support Email"
                  fullWidth
                  type="email"
                  {...register('general.supportEmail')}
                  error={!!errors.general?.supportEmail}
                  helperText={errors.general?.supportEmail?.message}
                />
                <TextField
                  label="Items Per Page"
                  type="number"
                  fullWidth
                  {...register('general.itemsPerPage', { valueAsNumber: true })}
                  error={!!errors.general?.itemsPerPage}
                  helperText={errors.general?.itemsPerPage?.message}
                  slotProps={{ htmlInput: { min: 5, max: 100 } }}
                />
              </>
            )}
          </Box>
        </Paper>

        {/* ---- Security Section ---- */}
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Security
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Authentication and session settings
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, maxWidth: 600 }}>
            {isLoading ? (
              <>
                <Skeleton variant="rectangular" height={56} />
                <Skeleton variant="rectangular" height={56} />
              </>
            ) : (
              <>
                <TextField
                  label="Session Timeout (minutes)"
                  type="number"
                  fullWidth
                  {...register('security.sessionTimeoutMinutes', { valueAsNumber: true })}
                  error={!!errors.security?.sessionTimeoutMinutes}
                  helperText={errors.security?.sessionTimeoutMinutes?.message}
                  slotProps={{ htmlInput: { min: 5, max: 1440 } }}
                />
                <TextField
                  label="Max Login Attempts"
                  type="number"
                  fullWidth
                  {...register('security.maxLoginAttempts', { valueAsNumber: true })}
                  error={!!errors.security?.maxLoginAttempts}
                  helperText={errors.security?.maxLoginAttempts?.message}
                  slotProps={{ htmlInput: { min: 1, max: 20 } }}
                />
              </>
            )}
          </Box>
        </Paper>

        {/* ---- Notifications Section ---- */}
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Notifications
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Configure notification channels
          </Typography>
          {isLoading ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Skeleton variant="rectangular" width={200} height={38} />
              <Skeleton variant="rectangular" width={200} height={38} />
            </Box>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              <Controller
                name="notifications.emailNotifications"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={
                      <Switch
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    }
                    label="Email Notifications"
                  />
                )}
              />
              <Typography variant="body2" color="text.secondary" sx={{ ml: 0 }}>
                Send order confirmations and alerts via email
              </Typography>

              <Divider sx={{ my: 0.5 }} />

              <Controller
                name="notifications.smsNotifications"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={
                      <Switch
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    }
                    label="SMS Notifications"
                  />
                )}
              />
              <Typography variant="body2" color="text.secondary" sx={{ ml: 0 }}>
                Send urgent alerts and shipping updates via SMS
              </Typography>
            </Box>
          )}
        </Paper>

        {/* ---- Save Button ---- */}
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          {isLoading ? (
            <Skeleton variant="rectangular" width={150} height={40} sx={{ borderRadius: 1 }} />
          ) : (
            <Button
              type="submit"
              variant="contained"
              size="large"
              startIcon={
                saveMutation.isPending ? (
                  <CircularProgress size={20} color="inherit" />
                ) : (
                  <SaveIcon />
                )
              }
              disabled={saveMutation.isPending || !isDirty}
            >
              {saveMutation.isPending ? 'Saving...' : 'Save Settings'}
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  )
}
