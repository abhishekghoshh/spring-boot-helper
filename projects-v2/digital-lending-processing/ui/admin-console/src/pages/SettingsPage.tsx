import { Box, Typography, Paper, Grid, TextField, Button, Switch, FormControlLabel, Divider } from '@mui/material'

export default function SettingsPage() {
  return (
    <Box maxWidth={800}>
      <Typography variant="h4" sx={{ fontWeight: 600 }} mb={4}>Settings</Typography>

      <Paper sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h6" gutterBottom>Platform Configuration</Typography>
        <Divider sx={{ mb: 3 }} />

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField fullWidth label="JWT Secret" type="password" defaultValue="••••••••" variant="outlined" disabled />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField fullWidth label="JWT Expiry (ms)" defaultValue="3600000" variant="outlined" disabled />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField fullWidth label="Default Interest Rate (%)" defaultValue="10.5" variant="outlined" disabled />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField fullWidth label="Max File Upload (MB)" defaultValue="10" variant="outlined" disabled />
          </Grid>
        </Grid>

        <Typography variant="h6" sx={{ mt: 4, mb: 1 }}>Feature Flags</Typography>
        <Divider sx={{ mb: 2 }} />

        <Stack spacing={2}>
          <FormControlLabel control={<Switch defaultChecked disabled />} label="Email Notifications" />
          <FormControlLabel control={<Switch defaultChecked disabled />} label="SMS Notifications" />
          <FormControlLabel control={<Switch defaultChecked disabled />} label="Auto-approve low-risk loans" />
          <FormControlLabel control={<Switch defaultChecked disabled />} label="Rate limiting" />
        </Stack>

        <Box mt={4}>
          <Button variant="contained" disabled sx={{ mr: 2 }}>Save Changes</Button>
          <Button variant="outlined" disabled>Reset Defaults</Button>
        </Box>
      </Paper>
    </Box>
  )
}
