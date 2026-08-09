import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Paper,
  CircularProgress,
  Box,
  Typography,
  TableSortLabel,
  TextField,
} from '@mui/material'
import { useState, useMemo, ReactNode } from 'react'

export interface Column<T> {
  id: string
  label: string
  minWidth?: number
  align?: 'left' | 'right' | 'center'
  sortable?: boolean
  format?: (value: unknown, row: T) => ReactNode
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  totalCount: number
  page: number
  rowsPerPage: number
  loading?: boolean
  error?: string | null
  searchPlaceholder?: string
  onSearch?: (search: string) => void
  onPageChange: (page: number) => void
  onRowsPerPageChange: (rowsPerPage: number) => void
  onSort?: (columnId: string, direction: 'asc' | 'desc') => void
  getRowId: (row: T) => string
}

export default function DataTable<T>({
  columns,
  rows,
  totalCount,
  page,
  rowsPerPage,
  loading = false,
  error = null,
  searchPlaceholder,
  onSearch,
  onPageChange,
  onRowsPerPageChange,
  onSort,
  getRowId,
}: DataTableProps<T>) {
  const [sortBy, setSortBy] = useState<string>('')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc')

  const handleSort = (columnId: string) => {
    const isAsc = sortBy === columnId && sortDir === 'asc'
    const newDir = isAsc ? 'desc' : 'asc'
    setSortBy(columnId)
    setSortDir(newDir)
    onSort?.(columnId, newDir)
  }

  const [searchValue, setSearchValue] = useState('')

  const handleSearchChange = (value: string) => {
    setSearchValue(value)
    onSearch?.(value)
  }

  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      {searchPlaceholder && onSearch && (
        <Box sx={{ p: 2 }}>
          <TextField
            fullWidth
            size="small"
            placeholder={searchPlaceholder}
            value={searchValue}
            onChange={(e) => handleSearchChange(e.target.value)}
            sx={{ maxWidth: 400 }}
          />
        </Box>
      )}
      {error && (
        <Box sx={{ p: 2 }}>
          <Typography color="error">{error}</Typography>
        </Box>
      )}
      <TableContainer sx={{ maxHeight: 600 }}>
        <Table stickyHeader size="small">
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell
                  key={col.id}
                  align={col.align}
                  style={{ minWidth: col.minWidth }}
                >
                  {col.sortable ? (
                    <TableSortLabel
                      active={sortBy === col.id}
                      direction={sortBy === col.id ? sortDir : 'asc'}
                      onClick={() => handleSort(col.id)}
                    >
                      {col.label}
                    </TableSortLabel>
                  ) : (
                    col.label
                  )}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={columns.length} align="center" sx={{ py: 4 }}>
                  <CircularProgress size={32} />
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">No records found</Typography>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow hover key={getRowId(row)}>
                  {columns.map((col) => (
                    <TableCell key={col.id} align={col.align}>
                      {col.format
                        ? col.format((row as Record<string, unknown>)[col.id], row)
                        : String((row as Record<string, unknown>)[col.id] ?? '')}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        rowsPerPageOptions={[10, 25, 50, 100]}
        component="div"
        count={totalCount}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={(_, newPage) => onPageChange(newPage)}
        onRowsPerPageChange={(e) =>
          onRowsPerPageChange(parseInt(e.target.value, 10))
        }
      />
    </Paper>
  )
}
