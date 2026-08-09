import { Chip } from '@mui/material'

type StatusColor =
  | 'success'
  | 'warning'
  | 'error'
  | 'info'
  | 'default'
  | 'primary'

interface StatusChipProps {
  status: string
  label?: string
}

const statusColorMap: Record<string, StatusColor> = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  DISCONTINUED: 'error',
  PENDING: 'warning',
  CONFIRMED: 'info',
  PROCESSING: 'info',
  SHIPPED: 'primary',
  DELIVERED: 'success',
  CANCELLED: 'error',
  REFUNDED: 'error',
  COMPLETED: 'success',
  FAILED: 'error',
  PARTIALLY_REFUNDED: 'warning',
  PAID: 'success',
  UNPAID: 'warning',
  VERIFIED: 'success',
  UNVERIFIED: 'warning',
}

const statusLabelMap: Record<string, string> = {
  PARTIALLY_REFUNDED: 'Partial Refund',
}

export default function StatusChip({ status, label }: StatusChipProps) {
  const color = statusColorMap[status] ?? 'default'
  const displayLabel =
    label ??
    statusLabelMap[status] ??
    status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()

  return (
    <Chip
      label={displayLabel}
      color={color}
      size="small"
      variant={color === 'default' ? 'outlined' : 'filled'}
    />
  )
}
