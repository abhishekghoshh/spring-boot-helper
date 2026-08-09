import { Typography, Breadcrumbs, Link as MuiLink, Box } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

interface BreadcrumbItem {
  label: string
  path?: string
}

interface HeaderProps {
  title: string
  breadcrumbs?: BreadcrumbItem[]
}

export default function Header({ title, breadcrumbs }: HeaderProps) {
  return (
    <Box sx={{ mb: 3 }}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumbs sx={{ mb: 1 }}>
          {breadcrumbs.map((crumb, index) => {
            const isLast = index === breadcrumbs.length - 1
            return isLast || !crumb.path ? (
              <Typography key={crumb.label} color="text.primary" variant="body2">
                {crumb.label}
              </Typography>
            ) : (
              <MuiLink
                key={crumb.label}
                component={RouterLink}
                to={crumb.path}
                underline="hover"
                color="inherit"
                variant="body2"
              >
                {crumb.label}
              </MuiLink>
            )
          })}
        </Breadcrumbs>
      )}
      <Typography variant="h4" sx={{ fontWeight: 600 }}>
        {title}
      </Typography>
    </Box>
  )
}
