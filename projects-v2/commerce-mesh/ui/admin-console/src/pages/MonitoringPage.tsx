import { useMemo } from 'react'
import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  LinearProgress,
  Skeleton,
  Alert,
  CircularProgress,
} from '@mui/material'
import MemoryIcon from '@mui/icons-material/Memory'
import CloudIcon from '@mui/icons-material/Cloud'
import StorageIcon from '@mui/icons-material/Storage'
import HubIcon from '@mui/icons-material/Hub'
import { useQuery } from '@tanstack/react-query'
import Header from '../components/Header'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type HealthStatus = 'healthy' | 'degraded' | 'down'

interface SystemHealthItem {
  name: string
  status: HealthStatus
  lastChecked: string
  icon: React.ReactNode
  uptime: string
  version: string
}

interface JvmStats {
  heapUsedMB: number
  heapMaxMB: number
  threadsLive: number
  threadsPeak: number
  cpuUsagePercent: number
  uptimeMinutes: number
}

interface ApiMetric {
  endpoint: string
  method: string
  requestsPerMin: number
  avgLatencyMs: number
  errorRate: number
}

interface RabbitMqQueue {
  queueName: string
  messages: number
  consumers: number
  status: 'running' | 'idle' | 'blocked'
}

interface MonitoringData {
  systemHealth: SystemHealthItem[]
  jvmStats: JvmStats
  apiMetrics: ApiMetric[]
  rabbitMqQueues: RabbitMqQueue[]
}

// ---------------------------------------------------------------------------
// Simulated fetch
// ---------------------------------------------------------------------------

function simulateDelay(ms = 2000): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchMonitoringData(): Promise<MonitoringData> {
  await simulateDelay(1200)

  const now = new Date().toISOString()

  const systemHealth: SystemHealthItem[] = [
    {
      name: 'API Gateway',
      status: 'healthy',
      lastChecked: now,
      icon: <CloudIcon />,
      uptime: '14d 7h 32m',
      version: 'v3.2.1',
    },
    {
      name: 'Database',
      status: 'healthy',
      lastChecked: now,
      icon: <StorageIcon />,
      uptime: '30d 2h 15m',
      version: 'PostgreSQL 16.1',
    },
    {
      name: 'Redis',
      status: 'healthy',
      lastChecked: now,
      icon: <MemoryIcon />,
      uptime: '14d 7h 28m',
      version: 'Redis 7.2.3',
    },
    {
      name: 'RabbitMQ',
      status: Math.random() > 0.85 ? 'degraded' : 'healthy',
      lastChecked: now,
      icon: <HubIcon />,
      uptime: '10d 18h 44m',
      version: 'RabbitMQ 3.12.10',
    },
  ]

  const jvmStats: JvmStats = {
    heapUsedMB: Math.round(280 + Math.random() * 200),
    heapMaxMB: 1024,
    threadsLive: Math.round(45 + Math.random() * 20),
    threadsPeak: Math.round(80 + Math.random() * 40),
    cpuUsagePercent: Math.round(15 + Math.random() * 30),
    uptimeMinutes: Math.round(12000 + Math.random() * 8000),
  }

  const apiMetrics: ApiMetric[] = [
    { endpoint: '/api/v1/products', method: 'GET', requestsPerMin: 342, avgLatencyMs: 45, errorRate: 0.2 },
    { endpoint: '/api/v1/orders', method: 'GET', requestsPerMin: 189, avgLatencyMs: 62, errorRate: 0.5 },
    { endpoint: '/api/v1/orders', method: 'POST', requestsPerMin: 87, avgLatencyMs: 145, errorRate: 1.2 },
    { endpoint: '/api/v1/auth/login', method: 'POST', requestsPerMin: 95, avgLatencyMs: 210, errorRate: 3.1 },
    { endpoint: '/api/v1/categories', method: 'GET', requestsPerMin: 156, avgLatencyMs: 32, errorRate: 0.0 },
    { endpoint: '/api/v1/inventory', method: 'GET', requestsPerMin: 134, avgLatencyMs: 55, errorRate: 0.1 },
    { endpoint: '/api/v1/payments', method: 'GET', requestsPerMin: 78, avgLatencyMs: 48, errorRate: 0.3 },
    { endpoint: '/api/v1/users', method: 'GET', requestsPerMin: 67, avgLatencyMs: 38, errorRate: 0.0 },
    { endpoint: '/api/v1/orders/status', method: 'PUT', requestsPerMin: 112, avgLatencyMs: 98, errorRate: 0.8 },
    { endpoint: '/api/v1/products/search', method: 'GET', requestsPerMin: 210, avgLatencyMs: 72, errorRate: 0.1 },
  ]

  const rabbitMqQueues: RabbitMqQueue[] = [
    { queueName: 'order.created', messages: 12, consumers: 2, status: 'running' },
    { queueName: 'order.cancelled', messages: 3, consumers: 1, status: 'idle' },
    { queueName: 'payment.processed', messages: 45, consumers: 3, status: 'running' },
    { queueName: 'inventory.updated', messages: 0, consumers: 2, status: 'idle' },
    { queueName: 'notification.email', messages: 8, consumers: 2, status: 'running' },
    { queueName: 'notification.sms', messages: 0, consumers: 1, status: 'idle' },
    { queueName: 'user.registered', messages: 2, consumers: 1, status: 'idle' },
  ]

  return { systemHealth, jvmStats, apiMetrics, rabbitMqQueues }
}

// ---------------------------------------------------------------------------
// Health Chip sub-component
// ---------------------------------------------------------------------------

function HealthChip({ status }: { status: HealthStatus }) {
  const colorMap: Record<HealthStatus, 'success' | 'warning' | 'error'> = {
    healthy: 'success',
    degraded: 'warning',
    down: 'error',
  }
  const labelMap: Record<HealthStatus, string> = {
    healthy: 'Healthy',
    degraded: 'Degraded',
    down: 'Down',
  }

  return (
    <Chip
      label={labelMap[status]}
      color={colorMap[status]}
      size="small"
    />
  )
}

// ---------------------------------------------------------------------------
// Status Chip sub-component (for RabbitMQ)
// ---------------------------------------------------------------------------

function QueueStatusChip({ status }: { status: 'running' | 'idle' | 'blocked' }) {
  const colorMap: Record<string, 'success' | 'default' | 'error'> = {
    running: 'success',
    idle: 'default',
    blocked: 'error',
  }
  const labelMap: Record<string, string> = {
    running: 'Running',
    idle: 'Idle',
    blocked: 'Blocked',
  }

  return (
    <Chip
      label={labelMap[status]}
      color={colorMap[status]}
      size="small"
      variant={status === 'idle' ? 'outlined' : 'filled'}
    />
  )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatUptime(minutes: number): string {
  const d = Math.floor(minutes / 1440)
  const h = Math.floor((minutes % 1440) / 60)
  const m = minutes % 60
  const parts: string[] = []
  if (d > 0) parts.push(`${d}d`)
  if (h > 0) parts.push(`${h}h`)
  parts.push(`${m}m`)
  return parts.join(' ')
}

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

export default function MonitoringPage() {
  const { data, isLoading, isError, error } = useQuery<MonitoringData>({
    queryKey: ['monitoring'],
    queryFn: fetchMonitoringData,
    refetchInterval: 30_000, // auto-refresh every 30s
  })

  const jvmHeapPercent = useMemo(() => {
    if (!data) return 0
    return Math.round((data.jvmStats.heapUsedMB / data.jvmStats.heapMaxMB) * 100)
  }, [data])

  // -------------------------------------------------------------------------

  if (isError) {
    return (
      <Box>
        <Header
          title="Monitoring"
          breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Monitoring' }]}
        />
        <Alert severity="error">
          {(error as Error)?.message || 'Failed to load monitoring data. Please try again.'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Header
        title="Monitoring"
        breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Monitoring' }]}
      />

      {/* ---- System Health Grid ---- */}
      <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>
        System Health
      </Typography>
      <Grid container spacing={2} sx={{ mb: 4 }}>
        {isLoading
          ? Array.from({ length: 4 }).map((_, i) => (
              <Grid size={{ xs: 12, sm: 6, md: 3 }} key={i}>
                <Card>
                  <CardContent sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                    <CircularProgress size={28} />
                  </CardContent>
                </Card>
              </Grid>
            ))
          : data!.systemHealth.map((item) => (
              <Grid size={{ xs: 12, sm: 6, md: 3 }} key={item.name}>
                <Card sx={{ height: '100%' }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ color: 'text.secondary' }}>{item.icon}</Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                          {item.name}
                        </Typography>
                      </Box>
                      <HealthChip status={item.status} />
                    </Box>
                    <Box sx={{ mt: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Uptime: {item.uptime}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Version: {item.version}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Last checked:{' '}
                        {new Date(item.lastChecked).toLocaleTimeString('en-US', {
                          hour: '2-digit',
                          minute: '2-digit',
                          second: '2-digit',
                        })}
                      </Typography>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
      </Grid>

      {/* ---- JVM Stats + API Metrics ---- */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* JVM Stats Card */}
        <Grid size={{ xs: 12, md: 5, lg: 4 }}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              JVM Statistics
            </Typography>
            {isLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : data ? (
              <Box>
                {/* Heap Usage */}
                <Box sx={{ mb: 3 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                    <Typography variant="body2" color="text.secondary">
                      Heap Usage
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {data.jvmStats.heapUsedMB} MB / {data.jvmStats.heapMaxMB} MB ({jvmHeapPercent}%)
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={jvmHeapPercent}
                    color={jvmHeapPercent > 80 ? 'error' : jvmHeapPercent > 60 ? 'warning' : 'primary'}
                    sx={{ height: 8, borderRadius: 4 }}
                  />
                </Box>

                {/* Threads */}
                <Box sx={{ mb: 2 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">
                      Threads (live / peak)
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {data.jvmStats.threadsLive} / {data.jvmStats.threadsPeak}
                    </Typography>
                  </Box>
                </Box>

                {/* CPU */}
                <Box sx={{ mb: 2 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">
                      CPU Usage
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {data.jvmStats.cpuUsagePercent}%
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={data.jvmStats.cpuUsagePercent}
                    color={data.jvmStats.cpuUsagePercent > 80 ? 'error' : data.jvmStats.cpuUsagePercent > 50 ? 'warning' : 'success'}
                    sx={{ height: 6, borderRadius: 3, mt: 0.5 }}
                  />
                </Box>

                {/* Uptime */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    JVM Uptime
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {formatUptime(data.jvmStats.uptimeMinutes)}
                  </Typography>
                </Box>
              </Box>
            ) : null}
          </Paper>
        </Grid>

        {/* API Metrics Table */}
        <Grid size={{ xs: 12, md: 7, lg: 8 }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              API Metrics
            </Typography>
            {isLoading ? (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600 }}>Endpoint</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>Req/min</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>Avg Latency</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>Error Rate</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {Array.from({ length: 6 }).map((_, i) => (
                      <TableRow key={i}>
                        <TableCell><Skeleton /></TableCell>
                        <TableCell align="right"><Skeleton /></TableCell>
                        <TableCell align="right"><Skeleton /></TableCell>
                        <TableCell align="right"><Skeleton /></TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : data && data.apiMetrics.length > 0 ? (
              <TableContainer sx={{ maxHeight: 340 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600 }}>Endpoint</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>Req/min</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>Avg Latency</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>Error Rate</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {data.apiMetrics.map((metric, idx) => (
                      <TableRow hover key={`${metric.method}-${metric.endpoint}-${idx}`}>
                        <TableCell>
                          <Chip label={metric.method} size="small" color="primary" variant="outlined" sx={{ mr: 1 }} />
                          <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                            {metric.endpoint}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography variant="body2">{metric.requestsPerMin.toLocaleString()}</Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography
                            variant="body2"
                            color={metric.avgLatencyMs > 150 ? 'error.main' : metric.avgLatencyMs > 80 ? 'warning.main' : 'text.primary'}
                          >
                            {metric.avgLatencyMs} ms
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography
                            variant="body2"
                            color={metric.errorRate > 2 ? 'error.main' : metric.errorRate > 0.5 ? 'warning.main' : 'success.main'}
                          >
                            {metric.errorRate.toFixed(1)}%
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                No API metrics available
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* ---- RabbitMQ Queues ---- */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          RabbitMQ Queues
        </Typography>
        {isLoading ? (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Queue Name</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Messages</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Consumers</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton /></TableCell>
                    <TableCell align="right"><Skeleton /></TableCell>
                    <TableCell align="right"><Skeleton /></TableCell>
                    <TableCell><Skeleton width={60} /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : data && data.rabbitMqQueues.length > 0 ? (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Queue Name</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Messages</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 600 }}>Consumers</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.rabbitMqQueues.map((queue) => (
                  <TableRow hover key={queue.queueName}>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                        {queue.queueName}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Typography
                        variant="body2"
                        sx={{ fontWeight: queue.messages > 10 ? 600 : 400 }}
                        color={queue.messages > 20 ? 'warning.main' : 'text.primary'}
                      >
                        {queue.messages.toLocaleString()}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2">{queue.consumers}</Typography>
                    </TableCell>
                    <TableCell>
                      <QueueStatusChip status={queue.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
            No queues available
          </Typography>
        )}
      </Paper>
    </Box>
  )
}
