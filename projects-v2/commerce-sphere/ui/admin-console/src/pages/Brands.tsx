import { useState } from 'react'
import { Box, Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, IconButton } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'; import DeleteIcon from '@mui/icons-material/Delete'
import { useBrands, useCreateBrand, useDeleteBrand } from '../hooks/useCategories'
import Loading from '../components/shared/Loading'

export default function Brands() {
  const { data, isLoading } = useBrands()
  const createBrand = useCreateBrand()
  const deleteBrand = useDeleteBrand()
  const [open, setOpen] = useState(false); const [name, setName] = useState(''); const [desc, setDesc] = useState('')

  return <>
    <Box display="flex" justifyContent="space-between" mb={3}><Typography variant="h4">Brands</Typography><Button startIcon={<AddIcon />} variant="contained" onClick={() => setOpen(true)}>Add</Button></Box>
    {isLoading ? <Loading /> : (
      <TableContainer component={Paper}><Table><TableHead><TableRow><TableCell>Name</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
        <TableBody>{data?.map((b: any) => (
          <TableRow key={b.id}><TableCell>{b.name}</TableCell><TableCell align="right"><IconButton color="error" onClick={() => { if (confirm('Delete?')) deleteBrand.mutate(b.id) }}><DeleteIcon /></IconButton></TableCell></TableRow>
        ))}</TableBody></Table></TableContainer>
    )}
    <Dialog open={open} onClose={() => setOpen(false)}><DialogTitle>New Brand</DialogTitle>
      <DialogContent><TextField fullWidth label="Name" margin="normal" value={name} onChange={e => setName(e.target.value)} /><TextField fullWidth label="Description" margin="normal" value={desc} onChange={e => setDesc(e.target.value)} /></DialogContent>
      <DialogActions><Button onClick={() => setOpen(false)}>Cancel</Button><Button variant="contained" onClick={() => { createBrand.mutate({ name, description: desc }); setOpen(false); setName(''); setDesc('') }}>Create</Button></DialogActions>
    </Dialog>
  </>
}
