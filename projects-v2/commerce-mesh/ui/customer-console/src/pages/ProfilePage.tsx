import { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  Avatar,
  Divider,
  Card,
  CardContent,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  Snackbar,
  Chip,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { useAuth } from '../hooks/useAuth';
import { authApi } from '../api/authApi';
import type { Address, User } from '../types';

const DUMMY_ADDRESSES: Address[] = [
  { id: '1', fullName: 'John Doe', street: '123 Main St', city: 'New York', state: 'NY', postalCode: '10001', phone: '+1 555-0100', isDefault: true },
];

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [addresses, setAddresses] = useState<Address[]>(DUMMY_ADDRESSES);

  const [addressDialogOpen, setAddressDialogOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);
  const [addrForm, setAddrForm] = useState<Address>({
    fullName: '',
    street: '',
    city: '',
    state: '',
    postalCode: '',
    phone: '',
    isDefault: false,
  });

  const handleSaveProfile = async () => {
    setSaving(true);
    setErrorMsg('');
    try {
      const updated = await authApi.updateProfile({ fullName, phone } as Partial<User>);
      updateUser(updated);
      setSuccessMsg('Profile updated successfully!');
    } catch {
      setErrorMsg('Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  const openAddAddress = () => {
    setEditingAddress(null);
    setAddrForm({ fullName: '', street: '', city: '', state: '', postalCode: '', phone: '', isDefault: false });
    setAddressDialogOpen(true);
  };

  const openEditAddress = (addr: Address) => {
    setEditingAddress(addr);
    setAddrForm(addr);
    setAddressDialogOpen(true);
  };

  const handleSaveAddress = () => {
    if (editingAddress) {
      setAddresses((prev) =>
        prev.map((a) => (a.id === editingAddress.id ? { ...addrForm, id: a.id } : a)),
      );
    } else {
      setAddresses((prev) => [...prev, { ...addrForm, id: String(Date.now()) }]);
    }
    setAddressDialogOpen(false);
    setSuccessMsg('Address saved!');
  };

  const handleDeleteAddress = (id: string) => {
    setAddresses((prev) => prev.filter((a) => a.id !== id));
    setSuccessMsg('Address removed.');
  };

  const handleSetDefault = (id: string) => {
    setAddresses((prev) =>
      prev.map((a) => ({ ...a, isDefault: a.id === id })),
    );
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        My Profile
      </Typography>

      <Grid container spacing={4}>
        <Grid item xs={12} md={6}>
          <Paper variant="outlined" sx={{ p: 4 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mb: 4 }}>
              <Avatar sx={{ width: 80, height: 80, bgcolor: 'primary.main', fontSize: 32 }}>
                {user?.fullName?.charAt(0) || user?.username?.charAt(0) || 'U'}
              </Avatar>
              <Box>
                <Typography variant="h6">{user?.fullName || user?.username}</Typography>
                <Typography color="text.secondary">{user?.email}</Typography>
                <Typography variant="caption" color="text.secondary">
                  Member since {new Date().getFullYear()}
                </Typography>
              </Box>
            </Box>

            <Divider sx={{ mb: 3 }} />

            {errorMsg && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMsg('')}>
                {errorMsg}
              </Alert>
            )}

            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Full Name"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  size="small"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth label="Email" value={user?.email || ''} size="small" disabled />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Phone"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  size="small"
                  placeholder="+1 234 567 8900"
                />
              </Grid>
              <Grid item xs={12}>
                <Button variant="contained" onClick={handleSaveProfile} disabled={saving}>
                  {saving ? 'Saving...' : 'Save Changes'}
                </Button>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper variant="outlined" sx={{ p: 4 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Typography variant="h6">Saved Addresses</Typography>
              <Button startIcon={<AddIcon />} size="small" onClick={openAddAddress}>
                Add New
              </Button>
            </Box>

            <Divider sx={{ mb: 2 }} />

            {addresses.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                No saved addresses.
              </Typography>
            ) : (
              addresses.map((addr) => (
                <Card key={addr.id} variant="outlined" sx={{ mb: 2, position: 'relative' }}>
                  <CardContent sx={{ '&:last-child': { pb: 2 } }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <Box>
                        <Typography variant="subtitle2" fontWeight={600}>
                          {addr.fullName}
                          {addr.isDefault && (
                            <Chip label="Default" size="small" color="primary" sx={{ ml: 1 }} />
                          )}
                        </Typography>
                        <Typography variant="body2">{addr.street}</Typography>
                        <Typography variant="body2">
                          {addr.city}, {addr.state} {addr.postalCode}
                        </Typography>
                        <Typography variant="body2">{addr.phone}</Typography>
                      </Box>
                      <Box sx={{ display: 'flex', gap: 0.5 }}>
                        <IconButton size="small" onClick={() => openEditAddress(addr)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" onClick={() => handleDeleteAddress(addr.id!)} color="error">
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>
                    {!addr.isDefault && (
                      <Button size="small" sx={{ mt: 1, textTransform: 'none' }} onClick={() => handleSetDefault(addr.id!)}>
                        Set as Default
                      </Button>
                    )}
                  </CardContent>
                </Card>
              ))
            )}
          </Paper>
        </Grid>
      </Grid>

      <Dialog open={addressDialogOpen} onClose={() => setAddressDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingAddress ? 'Edit Address' : 'Add Address'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField fullWidth label="Full Name" size="small" value={addrForm.fullName} onChange={(e) => setAddrForm({ ...addrForm, fullName: e.target.value })} />
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth label="Street Address" size="small" value={addrForm.street} onChange={(e) => setAddrForm({ ...addrForm, street: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="City" size="small" value={addrForm.city} onChange={(e) => setAddrForm({ ...addrForm, city: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="State" size="small" value={addrForm.state} onChange={(e) => setAddrForm({ ...addrForm, state: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Postal Code" size="small" value={addrForm.postalCode} onChange={(e) => setAddrForm({ ...addrForm, postalCode: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField fullWidth label="Phone" size="small" value={addrForm.phone} onChange={(e) => setAddrForm({ ...addrForm, phone: e.target.value })} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddressDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveAddress}>Save</Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!successMsg}
        autoHideDuration={3000}
        onClose={() => setSuccessMsg('')}
        message={successMsg}
      />
    </Box>
  );
}
